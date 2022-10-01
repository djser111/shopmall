package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("gulimall-cart")
public interface CartFeignService {
    @GetMapping("/updateItem")
    @ResponseBody
    public R updateItem(@RequestParam("skuId") Long skuId);
}
