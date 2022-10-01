package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            CartItemVo cartItemVo = new CartItemVo();
            CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.info(skuId);
                SkuInfoVo data = skuInfo.getData(new TypeReference<SkuInfoVo>() {
                });
                cartItemVo.setCount(num);
                cartItemVo.setImage(data.getSkuDefaultImg());
                cartItemVo.setPrice(data.getPrice());
                cartItemVo.setCheck(true);
                cartItemVo.setTitle(data.getSkuTitle());
                cartItemVo.setSkuId(skuId);
            }, executor);

            CompletableFuture<Void> getSkuSaleFuture = CompletableFuture.runAsync(() -> {
                R skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                List<String> data = skuSaleAttrValues.getData(new TypeReference<List<String>>() {
                });
                cartItemVo.setSkuAttr(data);
            }, executor);

            try {
                CompletableFuture.allOf(getSkuInfo, getSkuSaleFuture).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String s = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), s);
            return cartItemVo;
        } else {
            CartItemVo cartItemVo = JSON.parseObject(res, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            String s = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), s);
            return cartItemVo;
        }


    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(s, CartItemVo.class);
    }

    @Override
    public CartVo getCart() {
        CartVo cartVo = new CartVo();
        UserInfoVo userInfoVo = CartInterceptor.threadLocal.get();
        if (userInfoVo.getUserId() != null) {
            String cartKey = CART_PREFIX + userInfoVo.getUserId();
            List<CartItemVo> tempCartItems = getCartItems(CART_PREFIX + userInfoVo.getUserKey());
            if (tempCartItems != null && tempCartItems.size() > 0) {
                for (CartItemVo cartItem : tempCartItems) {
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
                clearCart(CART_PREFIX + userInfoVo.getUserKey());
            }
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        } else {
            String cartKey = CART_PREFIX + userInfoVo.getUserKey();
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        }
        cartVo.setCountNum();
        cartVo.setCountType();
        cartVo.setTotalAmount();
        return cartVo;
    }

    private List<CartItemVo> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = cartOps.values();
        if (values != null && values.size() > 0) {
            return values.stream().map(item -> {
                String s = (String) item;
                return JSON.parseObject(s, CartItemVo.class);
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItemVo> getUserCartItems() {
        UserInfoVo userInfoVo = CartInterceptor.threadLocal.get();
        if (userInfoVo == null) {
            return null;
        }
        String cartKey = CART_PREFIX + userInfoVo.getUserId();
        List<CartItemVo> cartItems = getCartItems(cartKey);

        if (cartItems != null && cartItems.size() > 0) {
            cartItems = cartItems.stream().filter(CartItemVo::getCheck).collect(Collectors.toList());
        }
        return cartItems;
    }

    @Override
    public void updateItem(Long skuId) {
        CartItemVo cartItem = getCartItem(skuId);
        if (cartItem != null) {
            Integer count = cartItem.getCount();
            deleteItem(skuId);
            addToCart(skuId, count);
        }
    }

    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoVo userInfoVo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoVo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoVo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoVo.getUserKey();
        }
        return redisTemplate.boundHashOps(cartKey);
    }
}
