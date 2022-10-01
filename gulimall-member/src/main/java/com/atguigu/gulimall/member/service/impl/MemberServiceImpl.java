package com.atguigu.gulimall.member.service.impl;

import com.atguigu.common.to.MemberRegisterTo;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.service.MemberLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegisterTo memberRegisterTo) {
        MemberEntity memberEntity = new MemberEntity();
        Long defaultLevel = memberLevelService.getDefaultLevel();
        memberEntity.setLevelId(defaultLevel);

        checkPhoneUnique(memberRegisterTo.getPhone());
        memberEntity.setMobile(memberRegisterTo.getPhone());

        checkUsernameUnique(memberRegisterTo.getUsername());
        memberEntity.setUsername(memberRegisterTo.getUsername());

        memberEntity.setNickname(memberRegisterTo.getUsername());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(memberRegisterTo.getPassword());
        memberEntity.setPassword(encode);

        memberEntity.setCreateTime(new Date());
        baseMapper.insert(memberEntity);

    }

    @Override
    public void checkUsernameUnique(String username) {
        Long count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count != 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) {
        Long count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count != 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public MemberEntity login(UserLoginTo userLoginTo) {
        String loginAcct = userLoginTo.getLoginAcct();
        String password = userLoginTo.getPassword();
        List<MemberEntity> memberEntities = baseMapper.selectList(new QueryWrapper<MemberEntity>().eq("username", loginAcct).or().eq("mobile", loginAcct).or().eq("email", loginAcct));
        if (memberEntities == null || memberEntities.size() == 0) {
            return null;
        }
        MemberEntity member = null;

        for (MemberEntity memberEntity : memberEntities) {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, memberEntity.getPassword());
            if (matches) {
                member = memberEntity;
            }
        }
        return member;

    }

    @Override
    public MemberEntity login(SocialUserTo socialUserTo) {
        String uid = socialUserTo.getUid();
        MemberEntity member = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (member != null) {
            //用户之前注册过
            member.setAccessToken(socialUserTo.getAccess_token());
            member.setExpiresIn(socialUserTo.getExpires_in());
            baseMapper.updateById(member);
            return member;
        }else {
            //新用户
            MemberEntity newMember = new MemberEntity();
            newMember.setAccessToken(socialUserTo.getAccess_token());
            newMember.setCreateTime(new Date());
            newMember.setLevelId(memberLevelService.getDefaultLevel());
            newMember.setExpiresIn(socialUserTo.getExpires_in());
            newMember.setSocialUid(socialUserTo.getUid());
            newMember.setHeader(socialUserTo.getAvatar_url());
            newMember.setNickname(socialUserTo.getName());
            baseMapper.insert(newMember);
            return newMember;
        }


    }

}