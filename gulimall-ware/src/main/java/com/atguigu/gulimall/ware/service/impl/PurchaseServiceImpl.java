package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDetailDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
@SuppressWarnings({"all"})
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0).or().eq("status", 1);
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            baseMapper.insert(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        if (baseMapper.selectById(purchaseId).getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || baseMapper.selectById(purchaseId).getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
            List<Long> items = mergeVo.getItems();
            Long finalPurchaseId = purchaseId;
            items.forEach(item -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(item);
                purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailsStatusEnum.ASSIGNED.getCode());
                purchaseDetailService.updateById(purchaseDetailEntity);
            });
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setUpdateTime(new Date());
            baseMapper.updateById(purchaseEntity);
        }
    }

    @Override
    @Transactional
    public void received(List<Long> ids) {
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            return baseMapper.selectById(id);
        }).filter(purchaseEntity -> {
            return purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
        }).peek(purchaseEntity -> {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            purchaseEntity.setUpdateTime(new Date());
        }).collect(Collectors.toList());
        this.updateBatchById(collect);
        collect.forEach(purchaseEntity -> {
            List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.list(new QueryWrapper<PurchaseDetailEntity>().eq("purchase_id", purchaseEntity.getId()));
            purchaseDetailEntities.forEach(purchaseDetailEntity -> {
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailsStatusEnum.BUYING.getCode());
                purchaseDetailService.updateById(purchaseDetailEntity);
            });
        });
    }

    @Override
    @Transactional
    public void done(PurchaseDoneVo purchaseDoneVo) {
        //2、更改采购项状态
        boolean flag = true;
        List<PurchaseDetailDoneVo> items = purchaseDoneVo.getItems();
        for (PurchaseDetailDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailsStatusEnum.HASERROR.getCode()) {
                flag = false;
            } else {
                PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(item.getItemId());
                List<WareSkuEntity> wareSkuEntities = wareSkuService.list(new QueryWrapper<WareSkuEntity>().eq("sku_id", purchaseDetail.getSkuId()).and(wareSkuEntityQueryWrapper -> {
                    wareSkuEntityQueryWrapper.eq("ware_id", purchaseDetail.getWareId());
                }));
                if (wareSkuEntities.size() == 0) {
                    WareSkuEntity wareSkuEntity = new WareSkuEntity();
                    wareSkuEntity.setSkuId(purchaseDetail.getSkuId());
                    wareSkuEntity.setWareId(purchaseDetail.getWareId());
                    Map<String, Object> map = (Map<String, Object>) productFeignService.info(purchaseDetail.getSkuId()).get("skuInfo");
                    wareSkuEntity.setSkuName((String) map.get("skuName"));
                    wareSkuEntity.setStock(purchaseDetail.getSkuNum());
                    wareSkuService.save(wareSkuEntity);
                } else {
                    wareSkuService.addStock(purchaseDetail.getSkuId(), purchaseDetail.getWareId(), purchaseDetail.getSkuNum());
                }
            }
            purchaseDetailEntity.setStatus(item.getStatus());
            purchaseDetailEntity.setId(item.getItemId());
            purchaseDetailService.updateById(purchaseDetailEntity);
        }

        //1、更改采购单状态
        PurchaseEntity purchaseEntity = baseMapper.selectById(purchaseDoneVo.getId());
        purchaseEntity.setUpdateTime(new Date());
        if (flag) {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISH.getCode());
        } else {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        }
        baseMapper.updateById(purchaseEntity);
    }
}