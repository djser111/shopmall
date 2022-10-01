package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.enume.OrderStatusEnum;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.OrderInfoTo;
import com.atguigu.common.to.OrderItemTo;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.mq.QuickOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.common.vo.OrderItemVo;
import com.atguigu.common.vo.OrderSpuInfoVo;
import com.atguigu.common.vo.WareSkuLockVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.UserLoginInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.to.SkuInfoTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Service("orderService")
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    public static ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = UserLoginInterceptor.loginUser.get();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            R address = memberFeignService.getAddress(memberRespVo.getId());
            List<MemberAddressVo> memberAddressVos = address.getData(new TypeReference<List<MemberAddressVo>>() {
            });
            orderConfirmVo.setAddress(memberAddressVos);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            R cartItems = cartFeignService.currentUserCartItems();
            List<OrderItemVo> orderItemVos = cartItems.getData(new TypeReference<List<OrderItemVo>>() {
            });
            orderConfirmVo.setItems(orderItemVos);
        }, executor).thenRunAsync(() -> {
            List<Long> skuIds = orderConfirmVo.getItems().stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            List<SkuHasStockTo> data = wareFeignService.getSkusHasStock(skuIds).getData(new TypeReference<List<SkuHasStockTo>>() {
            });
            Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
            orderConfirmVo.setStock(map);
        }, executor);

        try {
            CompletableFuture.allOf(addressFuture, cartFuture).get();
            orderConfirmVo.setIntegration(memberRespVo.getIntegration());
            orderConfirmVo.setTotal();
            orderConfirmVo.setPayPrice();

            String token = UUID.randomUUID().toString();
            orderConfirmVo.setOrderToken(token);
            redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return orderConfirmVo;
    }

    @Override
    @Transactional
    public SubmitOrderRespVo submitOrder(OrderSubmitVo orderSubmitVo) {
        submitVoThreadLocal.set(orderSubmitVo);
        SubmitOrderRespVo response = new SubmitOrderRespVo();
        response.setCode(0);
        MemberRespVo memberRespVo = UserLoginInterceptor.loginUser.get();
        String orderToken = orderSubmitVo.getOrderToken();
        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        //验证令牌：令牌的对比和删除必须保证原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L) {
            response.setCode(1);
            return response;
        }

        OrderCreateTo order = createOrder();
        BigDecimal payPrice = order.getPayPrice();
        BigDecimal voPayPrice = orderSubmitVo.getPayPrice();
        if (Math.abs(payPrice.subtract(voPayPrice).doubleValue()) < 0.01) {
            //保存订单
            saveOrder(order);
            WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
            wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
            List<OrderItemVo> locks = order.getOrderItems().stream().map(orderItem -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setSkuId(orderItem.getSkuId());
                orderItemVo.setCount(orderItem.getSkuQuantity());
                return orderItemVo;
            }).collect(Collectors.toList());
            wareSkuLockVo.setLocks(locks);
            R r = null;
            //远程锁库存
            r = wareFeignService.orderLockStock(wareSkuLockVo);
            if (r.getCode() == 0) {
                //锁定成功
                response.setOrder(order.getOrder());
                //订单创建成功，发送消息给mq
                rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                return response;
            } else {
                throw new NoStockException((String) r.get("msg"));
            }
        } else {
            response.setCode(2);
            return response;
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        return baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        Long id = entity.getId();
        OrderEntity orderEntity = baseMapper.selectById(id);
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity updateEntity = new OrderEntity();
            updateEntity.setId(id);
            updateEntity.setStatus(OrderStatusEnum.CANCLED.getCode());
            baseMapper.updateById(updateEntity);
            OrderInfoTo orderInfoTo = new OrderInfoTo();
            BeanUtils.copyProperties(orderEntity, orderInfoTo);
            try {
                //保证消息一定会发出去，每一个消息做好日志记录(给数据库保存每一个消息的详细信息)
                //定期扫描数据库，将失败的消息在发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other.#", orderInfoTo);
            } catch (AmqpException e) {
                //将没发送成功的消息进行重试发送
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {

        OrderEntity orderEntity = baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);
        BigDecimal bigDecimal = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        payVo.setBody(orderItemEntities.get(0).getSkuAttrsVals());
        payVo.setSubject(orderItemEntities.get(0).getSpuName());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = UserLoginInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId())
        );

        List<OrderInfoTo> orderInfoTos = page.getRecords().stream().map(orderEntity -> {
            List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderEntity.getOrderSn()));
            List<OrderItemTo> orderItemTos = orderItems.stream().map(orderItem -> {
                OrderItemTo orderItemTo = new OrderItemTo();
                BeanUtils.copyProperties(orderItem, orderItemTo);
                return orderItemTo;
            }).collect(Collectors.toList());

            OrderInfoTo orderInfoTo = new OrderInfoTo();
            BeanUtils.copyProperties(orderEntity, orderInfoTo);
            orderInfoTo.setOrderItems(orderItemTos);
            return orderInfoTo;
        }).collect(Collectors.toList());

        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(orderInfoTos);

        return pageUtils;

    }

    /**
     * 处理支付宝的返回结果
     *
     * @param payAsyncVo
     * @return
     */
    @Override
    public String handleAlipay(PayAsyncVo payAsyncVo) {
        //1、保存交易流水
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setOrderSn(payAsyncVo.getOut_trade_no());
        paymentInfoEntity.setAlipayTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setPaymentStatus(payAsyncVo.getTrade_status());
        paymentInfoEntity.setCallbackTime(payAsyncVo.getNotify_time());
        paymentInfoService.save(paymentInfoEntity);

        //2、修改订单的状态信息
        if (payAsyncVo.getTrade_status().equals("TRADE_SUCCESS") || payAsyncVo.getTrade_status().equals("TRADE_FINISHED")) {
            baseMapper.updateOrderStatus(payAsyncVo.getOut_trade_no(), OrderStatusEnum.PAYED.getCode());
        }

        //3、扣减库存
        String orderSn = payAsyncVo.getOut_trade_no();
        R r = wareFeignService.reduceStock(orderSn);
        if (r.getCode() == 0) {
            log.info("库存扣减成功");
        }
        return "success";
    }

    @Override
    public void seckillOrder(QuickOrderTo quickOrderTo) throws ExecutionException, InterruptedException {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(quickOrderTo.getOrderSn());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(new Date());
        orderEntity.setPayAmount(quickOrderTo.getSeckillPrice().multiply(new BigDecimal(quickOrderTo.getNum().toString())));
        orderEntity.setMemberId(quickOrderTo.getMemberId());
        orderEntity.setMemberUsername(quickOrderTo.getUserName());

        //TODO 保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(quickOrderTo.getOrderSn());

        CompletableFuture<Void> spuInfoFuture = CompletableFuture.runAsync(() -> {
            R spuInfoBySkuId = productFeignService.getSpuInfoBySkuId(quickOrderTo.getSkuId());
            if (spuInfoBySkuId.getCode() == 0) {
                OrderSpuInfoVo data = spuInfoBySkuId.getData(new TypeReference<OrderSpuInfoVo>() {
                });
                orderItemEntity.setSpuBrand(data.getBrandId().toString());
                orderItemEntity.setSpuName(data.getSpuName());
                orderItemEntity.setSpuId(data.getId());
                orderItemEntity.setCategoryId(data.getCatalogId());
            }
        }, executor);


        CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
            R info = productFeignService.info(quickOrderTo.getSkuId());
            if (info.getCode() == 0) {
                SkuInfoTo data = info.getData(new TypeReference<SkuInfoTo>() {
                });
                orderItemEntity.setSkuId(data.getSkuId());
                orderItemEntity.setSkuName(data.getSkuName());
                orderItemEntity.setSkuPic(data.getSkuDefaultImg());
                orderItemEntity.setSkuPrice(data.getPrice());
            }
        }, executor);

        CompletableFuture.allOf(spuInfoFuture, skuInfoFuture).get();
        baseMapper.insert(orderEntity);
    }

    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(new Date());
        baseMapper.insert(orderEntity);
        orderItemService.saveBatch(order.getOrderItems());
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();

        //构建订单编号
        OrderEntity orderEntity = buildOrder(orderSn);

        //获取所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        BigDecimal payPrice = null;
        if (itemEntities != null && itemEntities.size() > 0) {
            payPrice = computePrice(orderEntity, itemEntities);
        } else {
            payPrice = new BigDecimal("0");
        }


        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        orderCreateTo.setPayPrice(payPrice);
        orderCreateTo.setFare(orderEntity.getFreightAmount());

        return orderCreateTo;
    }

    private BigDecimal computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal totalPrice = new BigDecimal("0");
        BigDecimal promotionAmount = new BigDecimal("0");
        BigDecimal couponAmount = new BigDecimal("0");
        BigDecimal integrationAmount = new BigDecimal("0");
        int giftGrowth = 0;
        int giftIntegration = 0;
        for (OrderItemEntity item : itemEntities) {
            totalPrice = totalPrice.add(item.getRealAmount());
            promotionAmount = promotionAmount.add(item.getPromotionAmount());
            couponAmount = couponAmount.add(item.getCouponAmount());
            integrationAmount = integrationAmount.add(item.getIntegrationAmount());
            giftGrowth += item.getGiftGrowth();
            giftIntegration += item.getGiftIntegration();
        }
        orderEntity.setTotalAmount(totalPrice);
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotionAmount);
        orderEntity.setCouponAmount(couponAmount);
        orderEntity.setIntegrationAmount(integrationAmount);

        orderEntity.setGrowth(giftGrowth);
        orderEntity.setIntegration(giftIntegration);
        orderEntity.setDeleteStatus(0);

        return orderEntity.getPayAmount();
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        R userCartItems = cartFeignService.currentUserCartItems();
        if (userCartItems != null) {
            List<OrderItemVo> data = userCartItems.getData(new TypeReference<List<OrderItemVo>>() {
            });
            List<OrderItemEntity> itemEntities = data.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //构建sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";"));

        //构建spu信息
        R spuInfoBySkuId = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        if (spuInfoBySkuId != null) {
            OrderSpuInfoVo data = spuInfoBySkuId.getData(new TypeReference<OrderSpuInfoVo>() {
            });
            orderItemEntity.setSpuId(data.getId());
            orderItemEntity.setSpuBrand(data.getBrandId().toString());
            orderItemEntity.setSpuName(data.getSpuName());
            orderItemEntity.setCategoryId(data.getCatalogId());
        }

        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));

        BigDecimal multiply = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        orderItemEntity.setRealAmount(multiply.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount()).subtract(orderItemEntity.getPromotionAmount()));
        return orderItemEntity;
    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = UserLoginInterceptor.loginUser.get();
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberRespVo.getId());
        orderEntity.setMemberUsername(memberRespVo.getUsername());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //获取运费、地址信息
        R freight = wareFeignService.getFreight(orderSubmitVo.getAddrId());
        if (freight != null) {
            FareRespVo data = freight.getData(new TypeReference<FareRespVo>() {
            });
            //构建运费信息
            orderEntity.setFreightAmount(data.getFare());

            //构建收获地址信息
            MemberAddressVo address = data.getAddress();
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverDetailAddress(address.getDetailAddress());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
        }
        return orderEntity;
    }


}