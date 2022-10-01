package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.QuickOrderTo;
import com.atguigu.common.to.seckill.SeckillSessionTo;
import com.atguigu.common.to.seckill.SeckillSkuRelationTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.common.vo.SkuInfoVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.SeckillInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";//+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1、去数据库扫描最近三天需要参与秒杀的活动
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if (lates3DaySession.getCode() == 0) {
            //上架商品
            List<SeckillSessionTo> data = lates3DaySession.getData(new TypeReference<List<SeckillSessionTo>>() {
            });
            //缓存到redis
            //1、缓存活动信息
            saveSessionInfo(data);
            //2、缓存活动关联的商品信息
            saveSessionSkuInfo(data);
        }
    }

    @Override
    public List<SeckillSkuRelationTo> getCurrentSeckillSkus() {
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                String replace = key.replace(SESSION_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                if (time > Long.parseLong(s[0]) && time < Long.parseLong(s[1])) {
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> boundOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    List<String> objects = null;
                    if (range != null) {
                        objects = boundOps.multiGet(range);
                    }
                    if (objects != null) {
                        return objects.stream().map(item -> {
                            return JSON.parseObject(item, SeckillSkuRelationTo.class);
                        }).collect(Collectors.toList());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public SeckillSkuRelationTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null) {
            for (String key : keys) {
                String s = key.split("_")[1];
                if (skuId == Long.parseLong(s)) {
                    String o = hashOps.get(key);
                    SeckillSkuRelationTo seckillSkuRelationTo = JSON.parseObject(o, SeckillSkuRelationTo.class);
                    long time = new Date().getTime();
                    Long startTime = seckillSkuRelationTo != null ? seckillSkuRelationTo.getStartTime() : null;
                    Long endTime = seckillSkuRelationTo != null ? seckillSkuRelationTo.getEndTime() : null;
                    if (startTime != null && endTime != null && time >= startTime && time <= endTime) {
                        return seckillSkuRelationTo;
                    }
                    if (seckillSkuRelationTo != null) {
                        seckillSkuRelationTo.setRandomCode(null);
                    }
                    return seckillSkuRelationTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo memberRespVo = SeckillInterceptor.loginUser.get();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (!StringUtils.isEmpty(s)) {
            SeckillSkuRelationTo seckillSkuRelationTo = JSON.parseObject(s, SeckillSkuRelationTo.class);
            //1、校验合法性
            long time = new Date().getTime();
            Long startTime = seckillSkuRelationTo.getStartTime();
            Long endTime = seckillSkuRelationTo.getEndTime();
            if (time >= startTime && time <= endTime) {
                //2、校验随机码和商品id
                String randomCode = seckillSkuRelationTo.getRandomCode();
                String skuId = seckillSkuRelationTo.getPromotionSessionId() + "_" + seckillSkuRelationTo.getSkuId();
                if (key.equals(randomCode) && killId.equals(skuId)) {
                    //3、验证购物数量是否合理
                    if (num <= seckillSkuRelationTo.getSeckillCount()) {
                        //4、验证这个人是否已经购买过。幂等性；如果只要秒杀成功就去占位。userId_sessionId_skuId
                        String userKey = memberRespVo.getId() + "_" + skuId;
                        //5、自动过期
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(userKey, num.toString(), endTime - time, TimeUnit.MILLISECONDS);
                        if (Boolean.TRUE.equals(aBoolean)) {
                            //占位成功说明从来没有买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            //防止阻塞，秒杀不到不用等待
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功
                                //快速下单。发送mq消息
                                QuickOrderTo quickOrderTo = new QuickOrderTo();
                                String orderSn = IdWorker.getTimeId();
                                quickOrderTo.setOrderSn(orderSn);
                                quickOrderTo.setSeckillPrice(seckillSkuRelationTo.getSeckillPrice());
                                quickOrderTo.setMemberId(memberRespVo.getId());
                                quickOrderTo.setNum(num);
                                quickOrderTo.setPromotionSessionId(seckillSkuRelationTo.getPromotionSessionId());
                                quickOrderTo.setUserName(memberRespVo.getUsername());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", quickOrderTo);
                                return orderSn;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfo(List<SeckillSessionTo> data) {
        data.forEach(item -> {
            long startTime = item.getStartTime().getTime();
            long endTime = item.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            List<String> ids = item.getSeckillSkus().stream().map(activity -> {
                return activity.getPromotionSessionId().toString() + "_" + activity.getSkuId().toString();
            }).collect(Collectors.toList());
            //保存活动信息
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(hasKey)) {
                redisTemplate.opsForList().leftPushAll(key, ids);
            }
        });
    }

    private void saveSessionSkuInfo(List<SeckillSessionTo> data) {
        data.forEach(item -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            item.getSeckillSkus().forEach(seckillSkuRelationTo -> {
                if (Boolean.FALSE.equals(ops.hasKey(seckillSkuRelationTo.getSkuId().toString()))) {
                    //sku的基本信息
                    R info = productFeignService.info(seckillSkuRelationTo.getSkuId());
                    if (info.getCode() == 0) {
                        SkuInfoVo skuInfo = info.getData(new TypeReference<SkuInfoVo>() {
                        });
                        seckillSkuRelationTo.setSkuInfoVo(skuInfo);
                    }
                    //设置上当前商品的秒杀时间
                    seckillSkuRelationTo.setStartTime(item.getStartTime().getTime());
                    seckillSkuRelationTo.setEndTime(item.getEndTime().getTime());
                    //随机码
                    String randomCode = UUID.randomUUID().toString().replace("-", "");
                    seckillSkuRelationTo.setRandomCode(randomCode);

                    String s = JSON.toJSONString(seckillSkuRelationTo);
                    ops.put(seckillSkuRelationTo.getPromotionSessionId().toString() + "_" + seckillSkuRelationTo.getSkuId().toString(), s);
                    //使用库存作为分布式的信号量     限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuRelationTo.getSeckillCount());
                }
            });
        });
    }
}
