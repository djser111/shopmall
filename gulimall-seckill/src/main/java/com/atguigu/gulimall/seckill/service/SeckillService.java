package com.atguigu.gulimall.seckill.service;

import com.atguigu.common.to.seckill.SeckillSkuRelationTo;

import java.util.List;

public interface SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRelationTo> getCurrentSeckillSkus();

    SeckillSkuRelationTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
