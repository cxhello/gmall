package com.cxhello.gmall.cart.mapper;

import com.cxhello.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-14 11:48
 */
public interface CartInfoMapper extends Mapper<CartInfo> {

    List<CartInfo> selectCartListWithCurPrice(String userId);
}
