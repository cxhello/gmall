package com.cxhello.gmall.service;

import com.cxhello.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author CaiXiaoHui
 * @create 2019-07-17 19:15
 */
public interface PaymentInfoService {

    /**
     * 保存支付信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据对象中的属性查询数据
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 更新订单状态
     * @param paymentInfoUPD
     * @param out_trade_no
     */
    void updatePaymentInfo(PaymentInfo paymentInfoUPD, String out_trade_no);

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     *
     * @param orderId
     * @param total_fee
     * @return
     */
    Map createNative(String orderId, String total_fee);


    /**
     * 根据orderId、result进行发送消息
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 根据对象查询是否成功
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 发送延迟队列
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 根据订单Id 关闭交易记录
     * @param orderId
     */
    void closePaymentInfo(String orderId);
}
