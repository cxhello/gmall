package com.cxhello.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.cxhello.gmall.bean.OrderInfo;
import com.cxhello.gmall.bean.PaymentInfo;
import com.cxhello.gmall.bean.enums.PaymentStatus;
import com.cxhello.gmall.config.LoginRequire;
import com.cxhello.gmall.payment.config.AlipayConfig;
import com.cxhello.gmall.service.OrderInfoService;
import com.cxhello.gmall.service.PaymentInfoService;
import com.cxhello.gmall.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CaiXiaoHui
 * @create 2019-07-17 18:24
 */
@Controller
public class PaymentController {

    @Reference
    private OrderInfoService orderInfoService;

    @Reference
    private PaymentInfoService paymentInfoService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){

        String orderId = request.getParameter("orderId");

        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);

        //总金额保存
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        //订单ID保存
        request.setAttribute("orderId",orderId);

        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //获取订单ID
        String orderId = request.getParameter("orderId");

        //查询订单信息
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);

        //保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        //赋值
        paymentInfo.setOrderId(orderId);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());


        paymentInfoService.savePaymentInfo(paymentInfo);


        //支付宝参数,已注入到容器中
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient


        //1.创建API对应的request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //2.设置回调函数
        //同步
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //异步
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        //声明一个Map,使用map封装参数
        Map<String,Object> bizContnetMap=new HashMap<>();
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        bizContnetMap.put("subject",paymentInfo.getSubject());


        alipayRequest.setBizContent(JSON.toJSONString(bizContnetMap));


        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        // 在生成二维码的时候，开始主动询问支付宝的支付结果！每隔15秒一次，公共检查3次即可！
        paymentInfoService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    @RequestMapping("alipay/callback/return")
    public String callback(){
        // 重定向到订单url
        return "redirect:"+AlipayConfig.return_order_url;
    }


    @RequestMapping("alipay/callback/notify")
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramMap,HttpServletRequest request) throws AlipayApiException {

        boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8",AlipayConfig.sign_type);//调用SDK验证签名
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            String trade_status = paramMap.get("trade_status");
            // TODO 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                String out_trade_no = paramMap.get("out_trade_no");

                //查询当前交易支付状态 获取第三方交易编号，通过第三方交易编号，查询交易记录对象
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                //通过第三方交易编号，查询交易记录对象.
                PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(paymentInfoQuery);

                // 判断状态,如果交易关闭或者付款完成后就不用去更新状态了
                if (paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED || paymentInfo.getPaymentStatus()==PaymentStatus.PAID){
                    // 异常
                    return "failure";
                }

                //调用更新
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());

                //更新
                paymentInfoService.updatePaymentInfo(paymentInfoUPD,out_trade_no);

                // 通知订单支付成功？ Activemq 发送消息队列
                paymentInfoService.sendPaymentResult(paymentInfo,"success");


                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }


    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        boolean flag = paymentInfoService.refund(orderId);
        System.out.println("flag:"+flag);
        return flag+"";
    }

    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(String orderId){


        // 调用服务层数据
        // 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        IdWorker idworker=new IdWorker();
        Map map = paymentInfoService.createNative(idworker.nextId()+"", "1");
        System.out.println(map.get("code_url"));
        return map;

    }

    //测试  orderId=93&result=success
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){
        paymentInfoService.sendPaymentResult(paymentInfo,result);
        return "sendPaymentResult";
    }

    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean result = paymentInfoService.checkPayment(paymentInfoQuery);
        return result+"";
    }
}
