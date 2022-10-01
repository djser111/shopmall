package com.atguigu.gulimall.coupon;

import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.gulimall.coupon.controller.SpuBoundsController;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

@SpringBootTest
@RunWith(SpringRunner.class)
class GulimallCouponApplicationTests {
    @Autowired
    private SpuBoundsController spuBoundsController;

    @Test
    void contextLoads() {
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        spuBoundTo.setSpuId(1L);
        spuBoundTo.setBuyBounds(new BigDecimal(100));
        spuBoundTo.setGrowBounds(new BigDecimal(2));
        spuBoundsController.save(spuBoundTo);
    }

}
