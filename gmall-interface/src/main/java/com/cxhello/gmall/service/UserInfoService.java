package com.cxhello.gmall.service;

import com.cxhello.gmall.bean.UserAddress;
import com.cxhello.gmall.bean.UserInfo;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-01 21:01
 */
public interface UserInfoService {
    List<UserInfo> getUserInfoListAll();

    void insertUser(UserInfo userInfo);

    void updateUser(UserInfo userInfo);

    void updateUserByName(UserInfo userInfo);

    void delUser(UserInfo userInfo);

    void delUserByNickName(UserInfo userInfo);

    public List<UserAddress> getUserAddressList(String userId);

    /**
     * 登录
     * @param userInfo
     * @return
     */
    public UserInfo login(UserInfo userInfo);

    /**
     * 根据userId去查询redis中的用户信息
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
