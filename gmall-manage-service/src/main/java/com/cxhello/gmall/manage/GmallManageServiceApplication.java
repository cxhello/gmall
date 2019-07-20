package com.cxhello.gmall.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.cxhello.gmall.manage.mapper")
@EnableTransactionManagement
@ComponentScan(basePackages = "com.cxhello.gmall.config")
public class GmallManageServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(GmallManageServiceApplication.class, args);
    }

}
