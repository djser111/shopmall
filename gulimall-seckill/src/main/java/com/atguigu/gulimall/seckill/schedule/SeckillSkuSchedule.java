package com.atguigu.gulimall.seckill.schedule;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点，上架最近3天需要秒杀的商品
 * 当天00:00:00--->23:59:59
 * 明天00:00:00--->23:59:59
 * 后天00:00:00--->23:59:59
 */
@Service
@Slf4j
public class SeckillSkuSchedule {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    @Scheduled(cron = "0 0 3 * * ?")
    public void uploadSeckillSkuLatest3Days() throws InterruptedException {
        log.info("上架商品");
        //分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.tryLock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
}
