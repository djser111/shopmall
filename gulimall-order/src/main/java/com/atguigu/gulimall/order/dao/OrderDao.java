package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:31:14
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
