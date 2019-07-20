package com.cxhello.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.UserInfo;
import com.cxhello.gmall.passport.utils.JwtUtil;
import com.cxhello.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CaiXiaoHui
 * @create 2019-07-13 8:05
 */
@Controller
public class PassPortController {

    @Value("${token.key}")
    private String key;

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        //获取服务器的IP地址
        String salt = request.getHeader("X-forwarded-for");
        UserInfo loginUser = userInfoService.login(userInfo);
        if(loginUser!=null){
            //登录成功之后返回token
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",loginUser.getId());
            map.put("nickName",loginUser.getNickName());

            String token = JwtUtil.encode(key, map, salt);
            return token;
        }else{
            return "fail";
        }
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //http://passport.atguigu.com/verify?token=xxx&currentIp=xxx
        String token = request.getParameter("token");
        String salt = request.getParameter("currentIp");
        //调用jwt工具类
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if(map!=null && map.size()>0){
            String userId = (String) map.get("userId");

            //调用服务层方法去redis中验证是否有该用户信息
            UserInfo userInfo = userInfoService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }
}
