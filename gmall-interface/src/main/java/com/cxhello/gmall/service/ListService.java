package com.cxhello.gmall.service;

import com.cxhello.gmall.bean.SkuLsInfo;
import com.cxhello.gmall.bean.SkuLsParams;
import com.cxhello.gmall.bean.SkuLsResult;

/**
 * @author CaiXiaoHui
 * @create 2019-07-10 0:14
 */
public interface ListService {

    /**
     * 保存数据到ES中
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);


    /**
     * 根据用户输入的条件查询结果集
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);


    /**
     * 记录商品的访问次数
     * @param skuId
     */
    void incrHotScore(String skuId);

}
