package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.MemberRegisterTo;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * 会员
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:25:31
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUserTo socialUserTo) {
        MemberEntity login = memberService.login(socialUserTo);
        if (login == null) {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getMessage());
        }
        MemberRespVo memberRespVo = new MemberRespVo();
        BeanUtils.copyProperties(login, memberRespVo);
        return R.ok().setData(memberRespVo);
    }

    @PostMapping("login")
    public R login(@RequestBody UserLoginTo userLoginTo) {
        MemberEntity login = memberService.login(userLoginTo);
        if (login == null) {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getMessage());
        }
        MemberRespVo memberRespVo = new MemberRespVo();
        BeanUtils.copyProperties(login, memberRespVo);
        return R.ok().setData(memberRespVo);
    }

    @PostMapping("register")
    public R register(@RequestBody MemberRegisterTo memberRegisterTo) {
        try {
            memberService.register(memberRegisterTo);
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMessage());
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMessage());
        }
        return R.ok();
    }


    @RequestMapping("coupon")
    public R test() {
        R memberCoupons = couponFeignService.memberCoupons();
        return R.ok().put("data", memberCoupons.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
