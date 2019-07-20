package com.cxhello.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.SkuInfo;
import com.cxhello.gmall.bean.SkuLsInfo;
import com.cxhello.gmall.bean.SpuImage;
import com.cxhello.gmall.bean.SpuSaleAttr;
import com.cxhello.gmall.service.ListService;
import com.cxhello.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-06 16:20
 */
@RestController
@CrossOrigin
public class SkuManageController {


    @Reference
    private ManageService manageService;


    @Reference
    private ListService listService;

    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(String spuId){
        return manageService.getSpuImageList(spuId);
    }


    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrList(spuId);
    }

    @RequestMapping("saveSkuInfo")
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "OK";
    }

     @RequestMapping("onSale")
    public String onSale(String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuLsInfo(skuLsInfo);

        return "OK";
    }


}
