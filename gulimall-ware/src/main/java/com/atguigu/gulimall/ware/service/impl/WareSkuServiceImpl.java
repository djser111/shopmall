package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.enume.OrderStatusEnum;
import com.atguigu.common.to.OrderInfoTo;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.OrderItemVo;
import com.atguigu.common.vo.WareSkuLockVo;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private OrderFeignService orderFeignService;


    private void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        baseMapper.unlockStock(skuId, wareId, num);
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        baseMapper.addStock(skuId, wareId, skuNum);
    }

    @Override
    public List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockTo.setSkuId(skuId);
            skuHasStockTo.setHasStock(count != null && count > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {
        /**
         * ??????????????????????????????
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);

        List<OrderItemVo> locks = wareSkuLockVo.getLocks();

        List<SkuWareHasStock> skuWareHasStocks = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(item.getSkuId());
            stock.setWareId(wareIds);
            stock.setNum(item.getCount());
            stock.setSkuId(item.getSkuId());
            return stock;
        }).collect(Collectors.toList());
        //1???????????????????????????????????????????????????????????????????????????????????????????????????mq
        //2?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????id???????????????????????????
        for (SkuWareHasStock item : skuWareHasStocks) {
            boolean skuStocked = false;
            List<Long> wareIds = item.getWareId();
            Long skuId = item.getSkuId();
            Integer num = item.getNum();
            if (wareIds != null && wareIds.size() > 0) {
                for (Long wareId : wareIds) {
                    Long count = baseMapper.lockSkuStock(skuId, wareId, num);
                    if (count == 1) {
                        skuStocked = true;
                        //??????mq??????????????????
                        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", num, wareOrderTaskEntity.getId(), wareId, 1);
                        wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);

                        StockLockedTo stockLockedTo = new StockLockedTo();
                        stockLockedTo.setId(wareOrderTaskEntity.getId());
                        StockDetailTo stockDetailTo = new StockDetailTo();
                        BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                        //??????id???????????????????????????????????????
                        stockLockedTo.setDetail(stockDetailTo);
                        rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                        break;
                    }
                }
                if (!skuStocked) {
                    throw new NoStockException(skuId);
                }
            } else {
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        StockDetailTo detail = stockLockedTo.getDetail();
        Long detailId = detail.getId();
        /**
         * ??????
         * 1?????????????????????????????????????????????????????????
         *  ??????????????????????????????
         *      ?????????????????????
         *          1????????????????????????????????????
         *          2??????????????????????????????????????????
         *              ???????????????????????????????????????
         *                      ????????????????????????
         *  ?????????????????????????????????????????????????????????
         */
        WareOrderTaskDetailEntity taskDetailEntity = wareOrderTaskDetailService.getById(detailId);
        if (taskDetailEntity != null) {
            //????????????????????????????????????
            Long id = stockLockedTo.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            R orderByOrderSn = orderFeignService.getOrderByOrderSn(taskEntity.getOrderSn());
            if (orderByOrderSn.getCode() == 0) {
                //???????????????
                OrderInfoTo data = orderByOrderSn.getData(new TypeReference<OrderInfoTo>() {
                });
                if (data == null || data.getStatus() == OrderStatusEnum.CANCLED.getCode()) {
                    //??????????????????????????????
                    if (taskDetailEntity.getLockStatus() == 1) {
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                throw new RuntimeException("??????????????????");
            }
        }
    }

    //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    @Override
    @Transactional
    public void unlockStock(OrderInfoTo orderInfoTo) {
        String orderSn = orderInfoTo.getOrderSn();
        WareOrderTaskEntity orderTask = wareOrderTaskService.getOne(
                new QueryWrapper<WareOrderTaskEntity>()
                        .eq("order_sn", orderSn)
        );
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", orderTask.getId())
                        .eq("lock_status", 1)
        );
        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unlockStock(
                    taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId()
            );
        }
    }

    @Override
    public void reduceStock(String orderSn) {

        WareOrderTaskEntity orderTask = wareOrderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>().eq("order_sn", orderSn));
        List<WareOrderTaskDetailEntity> taskDetailEntities = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", orderTask.getId())
        );
        for (WareOrderTaskDetailEntity taskDetailEntity : taskDetailEntities) {
            WareSkuEntity wareSkuEntity = baseMapper.selectOne(new QueryWrapper<WareSkuEntity>().eq("sku_id", taskDetailEntity.getSkuId()));

            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setId(wareSkuEntity.getId());
            skuEntity.setSkuId(taskDetailEntity.getSkuId());
            skuEntity.setStock(wareSkuEntity.getStock() - taskDetailEntity.getSkuNum());
            skuEntity.setStockLocked(wareSkuEntity.getStockLocked() - taskDetailEntity.getSkuNum());
            baseMapper.updateById(skuEntity);
        }
    }

    @Data
    public static class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}