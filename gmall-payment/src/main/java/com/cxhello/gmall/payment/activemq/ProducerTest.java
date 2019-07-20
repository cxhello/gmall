package com.cxhello.gmall.payment.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;
import javax.smartcardio.CommandAPDU;

/**
 * @author CaiXiaoHui
 * @create 2019-07-18 14:30
 */
public class ProducerTest {

    public static void main(String[] args) throws JMSException {
        //1.创建连接工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.223.135:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        //2.创建session 参数1:表示是否开启事务,参数2:
        //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
        //3.创建队列
        Queue queue = session.createQueue("cxhello123");
        MessageProducer producer = session.createProducer(queue);
        //4.创建消息对象
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("HelloWorld");
        //5.发送消息
        producer.send(activeMQTextMessage);

        //
        session.commit();

        //6.关闭连接
        producer.close();
        session.close();
        connection.close();
    }

}
