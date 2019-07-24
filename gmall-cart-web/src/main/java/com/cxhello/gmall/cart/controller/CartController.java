package com.cxhello.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.CartInfo;
import com.cxhello.gmall.bean.SkuInfo;
import com.cxhello.gmall.config.LoginRequire;
import com.cxhello.gmall.service.CartInfoService;
import com.cxhello.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author CaiXiaoHui
 * @create 2019-07-14 11:32
 */
@Controller
public class CartController {

    @Reference
    private CartInfoService cartInfoService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;


    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");

        String skuNum = request.getParameter("skuNum");

        String skuId = request.getParameter("skuId");


        if(userId!=null){
            //用户登录情况下添加购物车
            cartInfoService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            //第一种方法:用户未登录情况下添加购物车,放入cookie中
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
            //第二种方法:用户未登录情况下添加购物车,放入redis中
            /*String userKey = "cart";
            String uuid=UUID.randomUUID().toString();
            Cookie[] cookies = request.getCookies();
            if(cookies!=null && cookies.length>0){
                for (Cookie cookie : cookies) {
                    if(userKey.equals(cookie.getName())){
                        uuid = cookie.getValue();
                    }else {
                        uuid = UUID.randomUUID().toString();
                    }
                }
            }
            Cookie cookie = new Cookie(userKey,uuid);
            //添加cookie
            response.addCookie(cookie);
            cartInfoService.addToCart(skuId,Integer.parseInt(skuNum),uuid);*/
        }

        //保存skuInfo

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo",skuInfo);

        request.setAttribute("skuNum",skuNum);

        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartInfoList = null;
        if(userId!=null){
            //合并购物车
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if(cartListCK!=null && cartListCK.size()>0){
                //调用合并方法
                cartInfoList = cartInfoService.mergeToCartList(cartListCK,userId);
                // 清空未登录数据
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                //登录
                cartInfoList = cartInfoService.getCartList(userId);
            }

        }else{
            //未登录
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request,HttpServletResponse response){

        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");

        if(userId!=null){
            //登录
            cartInfoService.checkCart(skuId,isChecked,userId);
        }else{
            //未登录
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");

        //获取未登录购物车数据
        List<CartInfo> cartListCk = cartCookieHandler.getCartList(request);
        if(cartListCk!=null && cartListCk.size()>0){
            //开始合并
            cartInfoService.mergeToCartList(cartListCk,userId);
            //删除cookie购物车
            cartCookieHandler.deleteCartCookie(request,response);
        }

        return "redirect://order.gmall.com/trade";
    }

}
