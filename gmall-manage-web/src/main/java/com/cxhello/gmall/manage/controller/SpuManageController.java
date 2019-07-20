package com.cxhello.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cxhello.gmall.bean.BaseSaleAttr;
import com.cxhello.gmall.bean.SpuInfo;
import com.cxhello.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-05 16:42
 */
@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuList")
    public List<SpuInfo> getSpuList(String catalog3Id){
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuList = manageService.getSpuList(spuInfo);
        return spuList;
    }

    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
    }
}
