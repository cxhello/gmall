package com.cxhello.gmall.order.mq;

import com.cxhello.gmall.bean.enums.ProcessStatus;
import com.cxhello.gmall.service.OrderInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author CaiXiaoHui
 * @create 2019-07-18 18:42
 */
@Component
public class OrderConsumer {

    @Autowired
    private OrderInfoService orderInfoService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("orderId:"+orderId);
        System.out.println("result:"+result);
        if("success".equals(result)){
            //1.更新订单状态
            orderInfoService.updateOrderStatus(orderId, ProcessStatus.PAID);
            //2.发送消息给库存
            orderInfoService.sendOrderStatus(orderId);
            //3.更新订单状态
            orderInfoService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }


    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        System.out.println("orderId:"+orderId);
        System.out.println("status:"+status);
        if("DEDUCTED".equals(status)){
            //更新订单状态
            orderInfoService.updateOrderStatus(orderId, ProcessStatus.FINISHED);
        }
    }



}
