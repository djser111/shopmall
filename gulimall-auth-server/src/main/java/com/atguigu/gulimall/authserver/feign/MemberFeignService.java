package com.atguigu.gulimall.authserver.feign;

import com.atguigu.common.to.MemberRegisterTo;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("member/member/register")
    public R register(@RequestBody MemberRegisterTo memberRegisterTo);

    @PostMapping("member/member/login")
    public R login(@RequestBody UserLoginTo userLoginTo);

    @PostMapping("member/member/oauth2/login")
    public R oauthLogin(@RequestBody SocialUserTo socialUserTo);
}
