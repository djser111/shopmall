package com.atguigu.gulimall.order.service;

import com.atguigu.common.to.mq.QuickOrderTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:31:14
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder();

    SubmitOrderRespVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handleAlipay(PayAsyncVo payAsyncVo);

    void seckillOrder(QuickOrderTo quickOrderTo) throws ExecutionException, InterruptedException;
}

