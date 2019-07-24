package com.cxhello.gmall.service;

import com.cxhello.gmall.bean.CartInfo;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-14 11:49
 */
public interface CartInfoService {

    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 根据用户id查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 勾选状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据用户ID查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户ID获取最新的价格
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId);

    /**
     *
     * @param skuId
     * @param skuNum
     * @param uuid
     */
    void addToCart(String skuId, Integer skuNum, String uuid);
}
