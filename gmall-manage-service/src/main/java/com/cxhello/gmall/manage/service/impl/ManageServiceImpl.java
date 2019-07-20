package com.cxhello.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cxhello.gmall.bean.*;
import com.cxhello.gmall.config.RedisUtil;
import com.cxhello.gmall.manage.constant.ManageConst;
import com.cxhello.gmall.manage.mapper.*;
import com.cxhello.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author CaiXiaoHui
 * @create 2019-07-03 9:08
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //首先判断是添加属性还是修改属性,修改属性有ID值,添加属性ID为空
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            //修改
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            //1.添加属性名
            //insert
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //修改属性值的时候,在修改的时候,先将已有的数据删除
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);
        //----------------------------------------------------

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //2.遍历属性值集合
        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue attrValue : attrValueList) {
                attrValue.setAttrId(baseAttrInfo.getId());
                //3.添加属性值
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }

    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //1.查询平台属性
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //2.查询平台属性值
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        //3.将平台属性值赋值给平台属性
        baseAttrInfo.setAttrValueList(baseAttrValueList);

        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 四张表:  spuInfo  spuImage  spuSaleAttr  spuSaleAttrValue
     *
     * @param spuInfo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo
        if (spuInfo.getId() != null && spuInfo.getId().length() > 0) {
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        } else {
            spuInfoMapper.insertSelective(spuInfo);
        }
        //spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        // spuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                // spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }

    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 四张表 skuInfo  skuImage  skuAttrValue  skuSaleAttrValue
     * @param skuInfo
     */
    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //1.skuImage
        if(skuInfo.getId()!=null && skuInfo.getId().length()>0){
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }else{
            skuInfoMapper.insertSelective(skuInfo);
        }

        //2.skuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList!=null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        //3.skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        //4.skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        //return getSkuInfoJedis(jedis,skuId);
        return getSkuInfoRedisson(skuId,jedis);
    }

    private SkuInfo getSkuInfoRedisson(String skuId, Jedis jedis) {
        SkuInfo skuInfo = null;
        RLock lock = null;
        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://192.168.223.135:6379");

            RedissonClient redissonClient = Redisson.create(config);

            lock = redissonClient.getLock("my-lock");

            lock.lock();

            //1.redisUtil
            jedis = redisUtil.getJedis();

            //2.定义Redis中的key
            String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            //3.进行判断
            if(jedis.exists(skuInfoKey)){
                //如果Redis中存在就取出数据
                String skuInfoJson = jedis.get(skuInfoKey);
                if(StringUtils.isNoneEmpty(skuInfoJson)){
                    //将字符串转换为对象
                    skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                    return skuInfo;
                }
            }else{
                //如果Redis中没有数据,则从数据库中查询,再放入到redis中
                SkuInfo skuInfoDB = getSkuInfoDB(skuId);
                String skuInfoDBStr = JSON.toJSONString(skuInfoDB);
                jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuInfoDBStr);
                return skuInfoDB;
            }

            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            if(jedis!=null){
                //4.关闭连接
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoJedis(Jedis jedis,String skuId) {
        SkuInfo skuInfo = null;
        try {
            //1.redisUtil
            /*jedis = redisUtil.getJedis();

            //2.定义Redis中的key
            String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            //3.进行判断
            if(jedis.exists(skuInfoKey)){
                //如果Redis中存在就取出数据
                String skuInfoJson = jedis.get(skuInfoKey);
                if(StringUtils.isNoneEmpty(skuInfoJson)){
                    //将字符串转换为对象
                    SkuInfo skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                    return skuInfo;
                }
            }else{
                //如果Redis中没有数据,则从数据库中查询,再放入到redis中
                SkuInfo skuInfoDB = getSkuInfoDB(skuId);
                String skuInfoDBStr = JSON.toJSONString(skuInfoDB);
                jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuInfoDBStr);
                return skuInfoDB;
            }*/


            //1.redisUtil
            jedis = redisUtil.getJedis();

            //2.定义Redis中的key
            String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            String skuInfoJson = jedis.get(skuInfoKey);
            if(skuInfoJson==null){
                System.out.println("缓存中没有数据");
                //上锁去数据库查询
                String skuLockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                //执行set()
                String lockKey = jedis.set(skuLockKey, "OK", "NX", "PX", ManageConst.SKUKEY_TIMEOUT);
                if("OK".equals(lockKey)){
                    System.out.println("获取分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    String skuInfoStr = JSON.toJSONString(skuInfo);
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuInfoStr);
                    return skuInfo;
                }else{
                    System.out.println("等待！");
                    // 等待
                    Thread.sleep(1000);
                    // 自旋
                    return getSkuInfo(skuId);
                }
            }else{
                //缓存中有数据
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                //4.关闭连接
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //将查询出来的skuImage集合放入skuInfo对象
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);

        skuInfo.setSkuImageList(skuImageList);

        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        //查询平台属性值集合
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {

        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");

        System.out.println("valueIds:"+valueIds);

        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
    }
}
