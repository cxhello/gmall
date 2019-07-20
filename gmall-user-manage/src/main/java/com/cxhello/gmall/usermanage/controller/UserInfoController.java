package com.cxhello.gmall.usermanage.controller;

import com.cxhello.gmall.bean.UserInfo;
import com.cxhello.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-01 20:59
 */
@RestController
public class UserInfoController {

    @Autowired
    private UserInfoService userService;


    @RequestMapping("findAll")
    public List<UserInfo> findAll() {
        return userService.getUserInfoListAll();
    }


    @RequestMapping("insert")
    @ResponseBody
    public void add() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@qq.com");
        userInfo.setLoginName("test");
        userInfo.setName("testQQ");
        userInfo.setPasswd("test");
        userService.insertUser(userInfo);
    }

    @RequestMapping("update")
    public void update() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("4");
        userInfo.setName("qq");
        userService.updateUser(userInfo);
    }

    @RequestMapping("updateByName")
    public void updateSecond(String name) {
        UserInfo userInfo = new UserInfo();
        userInfo.setNickName("sb");
        userInfo.setName("eeee");
        userService.updateUserByName(userInfo);
    }

    @RequestMapping("delete")
    public void delete() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("4");
        userService.delUser(userInfo);
    }

    @RequestMapping("deleteUserinfo")
    public void deleteUserinfo(UserInfo userInfo) {
        userService.delUser(userInfo);
    }

    @RequestMapping("deleteByNickName")
    public void deleteByNickName(UserInfo userInfo) {
        userService.delUserByNickName(userInfo);
    }
}
