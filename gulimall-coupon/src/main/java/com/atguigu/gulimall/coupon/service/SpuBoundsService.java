package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.to.SpuBoundTo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:16:30
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void save(SpuBoundTo spuBoundTo);
}

