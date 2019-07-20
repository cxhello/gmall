package com.cxhello.gmall.payment.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @author CaiXiaoHui
 * @create 2019-07-18 16:07
 */
public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        //1.创建连接工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_USER, ActiveMQConnectionFactory.DEFAULT_PASSWORD, "tcp://192.168.223.135:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        //2.创建Session
        //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

        //3.创建队列
        Queue queue = session.createQueue("cxhello123");
        //4.创建Consumer
        MessageConsumer consumer = session.createConsumer(queue);
        //5.接收消息
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if(message instanceof TextMessage){
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("接收到的消息:"+text);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
