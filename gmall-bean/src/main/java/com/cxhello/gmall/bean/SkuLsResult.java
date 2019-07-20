package com.cxhello.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-10 18:40
 */
@Data
public class SkuLsResult implements Serializable {

    List<SkuLsInfo> skuLsInfoList;

    long total;

    long totalPages;

    List<String> attrValueIdList;

}
