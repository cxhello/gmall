package com.cxhello.gmall.payment.activemq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.PaymentInfo;
import com.cxhello.gmall.service.PaymentInfoService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author CaiXiaoHui
 * @create 2019-07-20 18:06
 */
@Component
public class PaymentConsumer {

    @Reference
    private PaymentInfoService paymentInfoService;


    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        //获取消息队列中的数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        //判断是否支付成功 调用checkPayment
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        // 需要通过outTradeNo 再查询一下 才能得到orderId
        PaymentInfo paymentInfoQuery = paymentInfoService.getPaymentInfo(paymentInfo);
        //  outTradeNo 查询支付结果 orderId 根据orderId 发送消息给订单
        boolean result = paymentInfoService.checkPayment(paymentInfoQuery);
        // result 为false 表示没有支付成功！
        if(!result && checkCount>0){
            //继续发送消息
            paymentInfoService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }
    }
}
