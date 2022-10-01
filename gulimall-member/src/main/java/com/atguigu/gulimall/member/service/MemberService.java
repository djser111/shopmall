package com.atguigu.gulimall.member.service;

import com.atguigu.common.to.MemberRegisterTo;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.to.UserLoginTo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:25:31
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterTo memberRegisterTo);

    void checkUsernameUnique(String username);

    void checkPhoneUnique(String phone);

    MemberEntity login(UserLoginTo userLoginTo);

    MemberEntity login(SocialUserTo socialUserTo);
}

