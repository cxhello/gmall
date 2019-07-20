package com.cxhello.gmall.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author CaiXiaoHui
 * @create 2019-07-10 18:39
 */
@Data
public class SkuLsParams implements Serializable {
    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;

}
