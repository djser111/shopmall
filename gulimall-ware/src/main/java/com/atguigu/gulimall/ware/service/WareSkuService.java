package com.atguigu.gulimall.ware.service;

import com.atguigu.common.to.OrderInfoTo;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:34:37
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo wareSkuLockVo);

    void unlockStock(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException;

    void unlockStock(OrderInfoTo orderInfoTo);

    void reduceStock(String orderSn);
}

