package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 * 
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 16:25:31
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {

    Long getDefaultLevel();
}
