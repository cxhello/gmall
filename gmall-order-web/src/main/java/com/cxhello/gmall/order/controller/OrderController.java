package com.cxhello.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.*;
import com.cxhello.gmall.config.LoginRequire;
import com.cxhello.gmall.service.CartInfoService;
import com.cxhello.gmall.service.ManageService;
import com.cxhello.gmall.service.OrderInfoService;
import com.cxhello.gmall.service.UserInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author CaiXiaoHui
 * @create 2019-07-02 17:05
 */
@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartInfoService cartInfoService;


    @Reference
    private OrderInfoService orderInfoService;

    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){

        String userId = (String) request.getAttribute("userId");

        //获取购物车选中的商品
        List<CartInfo> cartInfoList = cartInfoService.getCartCheckedList(userId);
        //存储订单明细
        List<OrderDetail> orderDetailList = new ArrayList<>();
        if(cartInfoList!=null && cartInfoList.size()>0){
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                //属性赋值
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());

                orderDetailList.add(orderDetail);
            }
        }
        //存储在订单基本信息表里
        OrderInfo orderInfo = new OrderInfo();
        //设置订单明细
        orderInfo.setOrderDetailList(orderDetailList);
        //存储总金额
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        //存储订单明细
        request.setAttribute("orderDetailList",orderDetailList);

        List<UserAddress> userAddressList = userInfoService.getUserAddressList(userId);

        request.setAttribute("userAddressList",userAddressList);

        //调用生成流水号
        String tradeNo = orderInfoService.getTradeNo(userId);

        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }


    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){

        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        //计算总金额
        orderInfo.sumTotalAmount();

        //获取流水号进行判断
        String tradeNo = request.getParameter("tradeNo");

        boolean result = orderInfoService.checkTradeCode(userId, tradeNo);
        //true
        if(!result){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }

        //校验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderInfoService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

            //表示验证失败
            if(!flag){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"商品库存不足，请重新下单！");
                return "tradeFail";
            }

            //价格验证
            /*SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());

            if(res!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"价格有变动");
                return "tradeFail";
            }
            cartInfoService.loadCartCache(userId);*/

        }


        

        //删除流水号
        orderInfoService.delTradeCode(userId);

        String orderId = orderInfoService.saveOrder(orderInfo);

        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }


    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //获取子订单的集合
        List<OrderInfo> subOrderInfoList = orderInfoService.splitOrder(orderId,wareSkuMap);

        List<Map> mapList = new ArrayList<>();

        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderInfoService.initWareOrder(orderInfo);
            mapList.add(map);
        }
        // 返回子订单集合字符串
        return JSON.toJSONString(mapList);
    }
}
