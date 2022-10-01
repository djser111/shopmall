package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.vo.SkuItemSaleVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AttrGroupDao attrGroupDao;

    @Autowired
    private SkuSaleAttrValueDao skuSaleAttrValueDao;


    @Test
    void contextLoads() throws FileNotFoundException {
        System.out.println(redissonClient);
    }

    @Test
    void AttrGroupDao() {
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(4L, 225L);
        attrGroupWithAttrsBySpuId.forEach(System.out::println);
    }

    @Test
    void skuSaleAttrValueDao() {
        List<SkuItemSaleVo> attrGroupWithAttrsBySpuId = skuSaleAttrValueDao.getAttrGroupWithAttrsBySpuId(4L);
        attrGroupWithAttrsBySpuId.forEach(System.out::println);
    }

}
