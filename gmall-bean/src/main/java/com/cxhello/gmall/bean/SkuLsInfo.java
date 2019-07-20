package com.cxhello.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-10 0:11
 */
@Data
public class SkuLsInfo implements Serializable {

    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;

    //热度排名
    Long hotScore=0L;


    //平台属性值集合
    List<SkuLsAttrValue> skuAttrValueList;

}
