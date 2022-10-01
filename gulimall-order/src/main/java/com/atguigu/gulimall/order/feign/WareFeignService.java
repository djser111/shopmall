package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import com.atguigu.common.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {
    @PostMapping("ware/waresku/hasstock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds);

    @GetMapping("ware/wareinfo/getFreight")
    public R getFreight(@RequestParam("addrId") Long addrId);

    @PostMapping("ware/waresku/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo wareSkuLockVo);

    @PostMapping("ware/waresku/reduceStock")
    public R reduceStock(@RequestParam("orderSn") String orderSn);
}
