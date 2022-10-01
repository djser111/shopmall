package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author jiangdaiwei
 * @email 2638502607@qq.com
 * @date 2022-09-01 14:52:52
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addRelation(List<AttrGroupRelationVo> vos);

    void deleteBatchRelation(ArrayList<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntities);
}

