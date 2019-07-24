package com.cxhello.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.CartInfo;
import com.cxhello.gmall.bean.SkuInfo;
import com.cxhello.gmall.cart.constant.CartConst;
import com.cxhello.gmall.cart.mapper.CartInfoMapper;
import com.cxhello.gmall.config.RedisUtil;
import com.cxhello.gmall.service.CartInfoService;
import com.cxhello.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author CaiXiaoHui
 * @create 2019-07-14 11:51
 */
@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 用户登录情况下
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        if(cartInfoExist!=null){
            //说明购物车有该商品,更新数量即可
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //更新实时价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            cartInfoMapper.updateByPrimaryKey(cartInfoExist);
        }else {
            //说明购物车无该商品,需添加到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //插入到数据库中
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
        }
        //构建key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        String cartJson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartJson);
        //更新购物车时间***
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //1.查询缓存,如果缓存存在数据
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        List<CartInfo> cartInfoList = new ArrayList<>();

        List<String> cartInfoStrList = jedis.hvals(userCartKey);
        if(cartInfoStrList!=null && cartInfoStrList.size()>0){
            for (String cartInfoJson : cartInfoStrList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //细节处理,排序

            return cartInfoList;
        }else{
            //2.查询缓存,缓存中没有数据,去数据库中查询,然后再放到redis中
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        //获取数据库中的购物车
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        //循环遍历
        for (CartInfo cartInfoCk : cartListCK) {
            boolean isFlag = false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if(cartInfoCk.getSkuId().equals(cartInfoDB.getSkuId())){
                    //如果Cookie中的购物车商品和数据库中商品项id相等,数量相加
                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfoCk.getSkuNum());
                    //将数量更新到数据库中
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    //
                    isFlag=true;
                }
            }
            //如果Cookie中的购物车商品在数据库中没有,则添加这个商品到购物车
            if(!isFlag){
                cartInfoCk.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCk);
            }
        }
        // 重新在数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);
        //开始合并勾选状态的购物车
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCk : cartListCK) {
                if(cartInfoDB.getSkuId().equals(cartInfoCk.getSkuId())){
                    if("1".equals(cartInfoCk.getIsChecked())){
                        //开始合并
                        cartInfoDB.setIsChecked(cartInfoCk.getIsChecked());
                        //确认勾选商品
                        checkCart(cartInfoCk.getSkuId(),"1",userId);
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        Jedis jedis = redisUtil.getJedis();
        //定义Key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        if(StringUtils.isNotEmpty(cartJson)){
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartInfo.setIsChecked(isChecked);
            jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));

            //定义一个被选中的商品购物车key
            String userCartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
            if("1".equals(isChecked)){
                jedis.hset(userCartCheckedKey,skuId,JSON.toJSONString(cartInfo));
            }else{
                jedis.hdel(userCartCheckedKey,skuId);
            }
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        List<CartInfo> cartInfoList= new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        String userCartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;

        List<String> stringList = jedis.hvals(userCartCheckedKey);

        if(stringList!=null && stringList.size()>0){
            for (String cartJson : stringList) {
                cartInfoList.add(JSON.parseObject(cartJson,CartInfo.class));
            }
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> loadCartCache(String userId) {
        //在数据库中查询
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if(cartInfoList == null || cartInfoList.size()==0){
            return null;
        }
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        //循环集合添加到redis中
        for (CartInfo cartInfo : cartInfoList) {
            jedis.hset(userCartKey,cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }
        jedis.close();
        return cartInfoList;
    }

    @Override
    public void addToCart(String skuId, Integer skuNum, String uuid) {
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+uuid+CartConst.USER_CART_KEY_SUFFIX;
        //获取缓存中的所有数据
        Map<String, String> map = jedis.hgetAll(userCartKey);
        boolean isFlag = false;
        for (String sid : map.keySet()) {
            if(sid.equals(skuId)){
                String cartInfoJson = map.get(sid);
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                map.put(sid,JSON.toJSONString(cartInfo));
                isFlag=true;
            }
        }
        if(!isFlag){
            // 根据skuId 获取cartInfo
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(null);
            cartInfo.setSkuNum(skuNum);
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        // 将map 放入缓存
        jedis.hmset(userCartKey,map);
        jedis.close();
    }
}
