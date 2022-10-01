package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //获取sku的基本信息
    private SkuInfoEntity info;
    //商品是否有货
    private boolean hasStock = true;
    //获取sku的图片集
    private List<SkuImagesEntity> images;
    //获取spu的销售属性
    private List<SkuItemSaleVo> saleAttr;
    //获取spu的介绍
    private SpuInfoDescEntity desp;
    //获取spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;
    //当前商品的秒杀优惠信息
    private SeckillInfoVo seckillInfoVo;

}
