package com.cxhello.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.OrderInfo;
import com.cxhello.gmall.service.OrderInfoService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-20 20:01
 */
@EnableScheduling
@Component
public class OrderTask {


    @Reference
    private OrderInfoService orderInfoService;

    //开启定时任务、每五分钟的第五秒开始执行下面方法
    /*@Scheduled(cron = "5 * * * * ?")
    public void work1(){
        System.out.println(Thread.currentThread().getName()+"------");
    }

    //每隔五秒执行一次
    @Scheduled(cron = "0/5 * * * * ?")
    public void work2(){
        System.out.println(Thread.currentThread().getName()+"++++++");
    }*/


    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder(){
        List<OrderInfo> orderInfoList = orderInfoService.getExpiredOrderList();
        for (OrderInfo orderInfo : orderInfoList) {
            orderInfoService.execExpiredOrder(orderInfo);
        }
    }
}
