package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.vo.FareRespVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:34:37
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareRespVo getFreight(Long addrId);
}

