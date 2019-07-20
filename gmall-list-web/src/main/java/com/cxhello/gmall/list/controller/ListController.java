package com.cxhello.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cxhello.gmall.bean.*;
import com.cxhello.gmall.config.LoginRequire;
import com.cxhello.gmall.service.ListService;
import com.cxhello.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-11 15:47
 */
@Controller
@CrossOrigin
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;


    @RequestMapping("list.html")
    @LoginRequire//必须登录
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){

        //设置每页显示的条数
        skuLsParams.setPageSize(2);

        SkuLsResult search = listService.search(skuLsParams);



        //商品集合
        List<SkuLsInfo> skuLsInfoList = search.getSkuLsInfoList();



        // 将平台属性值ID集合传入
        List<String> attrValueIdList = search.getAttrValueIdList();

        List<BaseAttrInfo> baseAttrInfoList = null;
        if(attrValueIdList!=null && attrValueIdList.size()>0){
            baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        }else{

        }



        //制作一个查询的参数
        String urlParam = makeUrlParam(skuLsParams);

        request.setAttribute("urlParam",urlParam);

        System.out.println(urlParam);

        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();


        //使用迭代器
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            //平台属性值ID
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                //循环判断
                if(attrValueList!=null && attrValueList.size()>0){
                    //判断skuLsParams中的平台属性值不能为空

                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        //比较条件,平台属性值ID
                        for (String valueId : skuLsParams.getValueId()) {
                            if(valueId.equals(baseAttrValue.getId())){
                                //将平台属性对象移除
                                iterator.remove();
                                //制作面包屑 //创建平台值对象
                                BaseAttrValue baseAttrValue1 = new BaseAttrValue();
                                baseAttrValue1.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                                //点击让面包屑下来的ID,得到最新的urlParam
                                String makeUrlParam = makeUrlParam(skuLsParams, valueId);

                                baseAttrValue1.setUrlParam(makeUrlParam);

                                baseAttrValuesList.add(baseAttrValue1);
                            }
                        }
                    }
                }
            }
        }
        //关键词回显
        request.setAttribute("keyword",skuLsParams.getKeyword());

        //面包屑
        request.setAttribute("baseAttrValuesList",baseAttrValuesList);

        //保存集合到页面显示
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        //显示skuLsInfo集合
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        //
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        request.setAttribute("totalPages",search.getTotalPages());

        System.out.println(JSON.toJSONString(search));

        return "list";

    }

    /**
     * 制作urlParam
     * @param skuLsParams 用户输入的查询条件参数
     * @param excludeValueIds 用户点击面包屑获取的平台属性值
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            //拼接keyword
            urlParam += "keyword="+skuLsParams.getKeyword();
        }
        if(skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if(urlParam.length()>0){
                urlParam += "&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {
                //获取excludeValueIds点击的平台属性值ID

                if(excludeValueIds!=null && excludeValueIds.length>0){
                    //每次只能点击一次,所有每次获取第一个就可以
                    String excludeValueId = excludeValueIds[0];
                    //用户点击的平台属性值ID与原始的urlParam参数中的ID相同,则当前平台属性值不拼接
                    if(excludeValueId.equals(valueId)){
                        continue;//结束本次
                    }
                }

                if(urlParam.length()>0){
                    urlParam += "&";
                }
                urlParam+="valueId="+valueId;
            }
        }

        return urlParam;
    }


}
