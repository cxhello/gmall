package com.cxhello.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-03 8:52
 */
@Data
public class BaseAttrInfo implements Serializable {
    //获取主键自增
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    //标识为不是数据库的字段
    @Transient
    private List<BaseAttrValue> attrValueList;
}
