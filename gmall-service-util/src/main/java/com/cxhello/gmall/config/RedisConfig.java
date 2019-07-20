package com.cxhello.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author CaiXiaoHui
 * @create 2019-07-09 7:57
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:disabled}")
    public String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean
    public RedisUtil getRedisUtil(){

        if("".equals(host)){
            return null;
        }

        RedisUtil redisUtil = new RedisUtil();
        //初始化连接池
        redisUtil.initJedisPool(host,port,database);

        return redisUtil;
    }

}
