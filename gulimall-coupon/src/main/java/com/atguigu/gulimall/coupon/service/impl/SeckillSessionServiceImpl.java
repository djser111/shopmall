package com.atguigu.gulimall.coupon.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.atguigu.common.to.seckill.SeckillSkuRelationTo;
import com.atguigu.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.atguigu.gulimall.coupon.service.SeckillSkuRelationService;
import com.atguigu.common.to.seckill.SeckillSessionTo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.SeckillSessionDao;
import com.atguigu.gulimall.coupon.entity.SeckillSessionEntity;
import com.atguigu.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionTo> getLates3DaySession() {
        DateTime date = DateUtil.date();
        DateTime begin = DateUtil.beginOfDay(date);
        DateTime newDate = DateUtil.offsetDay(date, 2);
        DateTime end = DateUtil.endOfDay(newDate);
        List<SeckillSessionEntity> seckillSessions = baseMapper.selectList(
                new QueryWrapper<SeckillSessionEntity>()
                        .between("start_time", begin, end)
        );
        return seckillSessions.stream().map(seckillSession -> {
            SeckillSessionTo seckillSessionTo = new SeckillSessionTo();
            Long id = seckillSession.getId();
            List<SeckillSkuRelationEntity> skuRelationEntities = seckillSkuRelationService.list(
                    new QueryWrapper<SeckillSkuRelationEntity>()
                            .eq("promotion_session_id", id)
            );
            List<SeckillSkuRelationTo> skuRelationTos = skuRelationEntities.stream().map(seckillSkuRelationEntity -> {
                SeckillSkuRelationTo seckillSkuRelationTo = new SeckillSkuRelationTo();
                BeanUtils.copyProperties(seckillSkuRelationEntity, seckillSkuRelationTo);
                return seckillSkuRelationTo;
            }).collect(Collectors.toList());

            BeanUtils.copyProperties(seckillSession, seckillSessionTo);
            seckillSessionTo.setSeckillSkus(skuRelationTos);
            return seckillSessionTo;
        }).collect(Collectors.toList());
    }
}