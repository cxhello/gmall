package com.cxhello.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.SkuInfo;
import com.cxhello.gmall.bean.SkuSaleAttrValue;
import com.cxhello.gmall.bean.SpuSaleAttr;
import com.cxhello.gmall.config.LoginRequire;
import com.cxhello.gmall.service.ListService;
import com.cxhello.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-07 16:54
 */
@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, HttpServletRequest request){

        System.out.println("商品ID:"+skuId);

        //1.存储基本的skuInfo信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo",skuInfo);

        //2.存储spu,sku数据,包含了销售属性，销售属性值
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("saleAttrList",saleAttrList);

        //3.根据spuId 查询与skuId 有关的销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //在后台拼接字符串 113|117=33  114|117=34  114|118=35 将其先组合成map,然后把map转换为json

        String key = "";

        HashMap<String,Object> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);

            //如果长度大于0,再拼接 |
            if(key.length()>0){
                key+="|";
            }

            //第一次拼接 key = "" + 113 , key = 113
            key += skuSaleAttrValue.getSaleAttrValueId();

            //第二次拼接 key = 113 + "|", key = 113|

            //第三次拼接 key = 113| + 117 key= 113|117

            //第四次拼接,将key放入map中,map.put(key,skuId);然后将key清空,再次拼接

            //当skuId不相同的时候,拼接到集合最后的时候,就将key放入map

            //与下一个skuId不同时,就放入
            if((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                map.put(key,skuSaleAttrValue.getSkuId());
                key = "";
            }


        }

        //将map转换为json
        String valuesSkuJson  = JSON.toJSONString(map);

        System.out.println(valuesSkuJson);

        request.setAttribute("valuesSkuJson",valuesSkuJson);

        //调用热度排名
        listService.incrHotScore(skuId);

        return "item";
    }
}
