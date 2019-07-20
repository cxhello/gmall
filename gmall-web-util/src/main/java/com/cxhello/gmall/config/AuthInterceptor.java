package com.cxhello.gmall.config;

import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.utils.CookieUtil;
import com.cxhello.gmall.utils.HttpClientUtil;
import com.cxhello.gmall.utils.WebConst;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author CaiXiaoHui
 * @create 2019-07-13 20:01
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 进入控制器之前执行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getParameter("newToken");
        //1.登录之后,保存到cookie中
        if(token!=null){
            CookieUtil.setCookie(request,response,"token",token, WebConst.COOKIE_MAXAGE,false);
        }
        //2.再次访问商品列表页面,如果没有直接从cookie中获取
        if(token==null){
            token = CookieUtil.getCookieValue(request,"token",false);
        }
        //3.获取到token,解密token,获取用户的姓名
        if(token!=null){
            // 解密token 获取用户昵称,使用base64 编码
            Map map = getUserMapByToken(token);

            String nickName = (String) map.get("nickName");

            request.setAttribute("nickName",nickName);
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取方法头上的注解是否存在
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if(methodAnnotation!=null){
            //注解存在,直接进行认证调用verify控制器
            //获取IP
            String currentIp = request.getHeader("X-forwarded-for");

            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + currentIp);
            if("success".equals(result)){
                //认证成功,说明用户属于登录状态
                // 保存用户Id
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                // 将用户昵称先保存到作用域
                request.setAttribute("userId",userId);

                return true;
            }else{
                //认证失败
                if(methodAnnotation.autoRedirect()){
                    //必须登录
                    //http://passport.atguigu.com/index?originUrl=http%3A%2F%2Flist.gmall.com%2Flist.html%3Fkeyword%3D%25E5%25B0%258F%25E7%25B1%25B3
                    //获取当前请求的地址
                    String requestURL = request.getRequestURL().toString();
                    System.out.println(requestURL);
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println(encodeURL);
                    //页面跳转
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 解密token中的第二段,用户信息
     * @param token
     * @return
     */
    private Map getUserMapByToken(String token) {
        String userInfoToken = StringUtils.substringBetween(token, ".");
        //创建Base64对象进行解密
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decode = base64UrlCodec.decode(userInfoToken);
        String userInfoJson = null;
        try {
            userInfoJson = new String(decode,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //将字符串转换为Map
        Map map = JSON.parseObject(userInfoJson, Map.class);
        return map;
    }

    /**
     * 进入控制器后,视图解析之前执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    /**
     * 视图解析完成后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

}
