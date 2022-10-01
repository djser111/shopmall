package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @GetMapping("member/memberreceiveaddress/{memberId}/getAddress")
    public R getAddress(@PathVariable("memberId") Long memberId);
}
