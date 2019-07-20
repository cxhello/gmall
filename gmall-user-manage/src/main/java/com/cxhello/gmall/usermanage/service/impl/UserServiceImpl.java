package com.cxhello.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.UserAddress;
import com.cxhello.gmall.bean.UserInfo;
import com.cxhello.gmall.config.RedisUtil;
import com.cxhello.gmall.usermanage.mapper.UserAddressMapper;
import com.cxhello.gmall.usermanage.mapper.UserInfoMapper;
import com.cxhello.gmall.service.UserInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-01 21:01
 */
@Service
public class UserServiceImpl implements UserInfoService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;
    
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<UserInfo> getUserInfoListAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public void insertUser(UserInfo userInfo) {
        userInfoMapper.insert(userInfo);
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKey(userInfo);
    }

    @Override
    public void updateUserByName(UserInfo userInfo) {

        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",userInfo.getName());
        userInfoMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void delUser(UserInfo userInfo) {
        userInfoMapper.delete(userInfo);
    }

    @Override
    public void delUserByNickName(UserInfo userInfo) {
        Example example = new Example(userInfo.getClass());
        example.createCriteria().andEqualTo("nickName",userInfo.getNickName());
        userInfoMapper.deleteByExample(example);
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //1.加密
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        //2.去数据库查询
        UserInfo info = userInfoMapper.selectOne(userInfo);

        //3.判断
        if(info!=null){
            //获取redis对象
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String key = userKey_prefix+userId+userinfoKey_suffix;

        String userJson = jedis.get(key);
        if(StringUtils.isNotEmpty(userJson)){
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            //延长用户的过期时间
            jedis.expire(key,userKey_timeOut);

            jedis.close();
            return userInfo;
        }

        return null;
    }
}
