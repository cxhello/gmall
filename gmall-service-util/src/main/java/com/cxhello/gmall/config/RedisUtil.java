package com.cxhello.gmall.config;

import org.springframework.context.annotation.ComponentScan;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis工具类
 * @author CaiXiaoHui
 * @create 2019-07-08 23:25
 */
public class RedisUtil {

    //1.创建连接池
    private JedisPool jedisPool = null;

    //创建一个初始化方法
    //JedisPool = new JedisPool();

    public void initJedisPool(String host,int port,int database){
        //配置连接池参数对象
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        //最大连接数
        jedisPoolConfig.setMaxTotal(200);

        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);

        //设置最小剩余数
        jedisPoolConfig.setMinIdle(30);

        //设置开机自检*****
        jedisPoolConfig.setTestOnBorrow(true);

        //如果达到最大连接数,需要等待
        jedisPoolConfig.setBlockWhenExhausted(true);

        //

        jedisPool = new JedisPool(jedisPoolConfig,host,port,20*1000);
    }

    //获取Jedis
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }


}
