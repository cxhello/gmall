package com.cxhello.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.OrderDetail;
import com.cxhello.gmall.bean.OrderInfo;
import com.cxhello.gmall.bean.enums.OrderStatus;
import com.cxhello.gmall.bean.enums.ProcessStatus;
import com.cxhello.gmall.config.ActiveMQUtil;
import com.cxhello.gmall.config.RedisUtil;
import com.cxhello.gmall.order.mapper.OrderDetailMapper;
import com.cxhello.gmall.order.mapper.OrderInfoMapper;
import com.cxhello.gmall.service.OrderInfoService;
import com.cxhello.gmall.service.PaymentInfoService;
import com.cxhello.gmall.utils.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

/**
 * @author CaiXiaoHui
 * @create 2019-07-16 18:23
 */
@Service
public class OrderInfoServiceImpl implements OrderInfoService {


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentInfoService paymentInfoService;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {

        // 向两张表插入数据 orderInfo ,orderDetail
        // orderInfo ,totalAmount, order_status,user_id,out_trade_no,createTime,expireTime,process_status,
        orderInfo.setCreateTime(new Date());
        //设置过期时间为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //设置状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //设置第三方交易编号
        String outTradeNo="CXHELLO"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfoMapper.insertSelective(orderInfo);

        //插入orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }

        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = UUID.randomUUID().toString();

        jedis.set(tradeNoKey,tradeCode);

        jedis.close();

        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();

        return tradeCodeNo.equals(tradeCode);
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {

        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(result);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();

        orderDetail.setOrderId(orderId);

        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();

        String orderJson = initWareOrder(orderId);

        try {
            connection.start();
            Session session = connection.createSession(true,Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");

            MessageProducer producer = session.createProducer(order_result_queue);

            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

            activeMQTextMessage.setText(orderJson);

            producer.send(activeMQTextMessage);

            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // orderStatus=UNPAID and exipreTime < new Date();
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());
        return orderInfoMapper.selectByExample(example);
    }

    @Override
    @Async//实现异步操作
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 修改订单状态
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 如果有对应的交易记录，则需要关闭交易记录
        paymentInfoService.closePaymentInfo(orderInfo.getId());
    }

    private String initWareOrder(String orderId) {

        OrderInfo orderInfo = getOrderInfo(orderId);

        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    @Override
    public Map initWareOrder(OrderInfo orderInfo) {

        Map<String, Object> map = new HashMap<>();

        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consignee_tel",orderInfo.getConsigneeTel());
        map.put("delivery_address",orderInfo.getDeliveryAddress());
        map.put("order_comment",orderInfo.getOrderComment());
        map.put("payment_way","2");
        map.put("order_body",orderInfo.getTradeBody());
        map.put("wareId",orderInfo.getWareId());

        List<Map> detailList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                Map orderDetailMap = new HashMap();
                orderDetailMap.put("skuId",orderDetail.getSkuId());
                orderDetailMap.put("skuName",orderDetail.getSkuName());
                orderDetailMap.put("skuNum",orderDetail.getSkuNum());
                detailList.add(orderDetailMap);
            }
        }
        map.put("details",detailList);
        return map;
    }

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
        1.  先获取到原始订单
        2.  wareSkuMap 转换成我们能操作的对象
        3.  创建新的子订单
        4.  给子订单赋值
        5.  保存到数据库
        6.  添加到子订单集合
        7.  修改订单状态
         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);

        for (Map map : mapList) {
            // 获取仓库Id
            String wareId = (String) map.get("wareId");
            // 获取商品Id
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 创建新的子订单
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            subOrderInfo.setWareId(wareId);

            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

            // 创建一个新的子订单明细
            ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();


            // 循环原始订单明细与"skuIds":["2","10"] 对应的商品Id 进行比较
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if(orderDetail.getSkuId().equals(skuId)){
                        orderDetail.setId(null);
                        orderDetailArrayList.add(orderDetail);
                    }
                }
            }

            // 子订单明细集合赋值给子订单
            subOrderInfo.setOrderDetailList(orderDetailArrayList);

            // 可以计算子订单金额
            subOrderInfo.sumTotalAmount();

            // 保存到数据
            saveOrder(subOrderInfo);

            // 将新的子订单添加到集合中
            subOrderInfoList.add(subOrderInfo);
        }

        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }

}
