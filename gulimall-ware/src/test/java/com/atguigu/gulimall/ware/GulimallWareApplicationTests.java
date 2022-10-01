package com.atguigu.gulimall.ware;

import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
class GulimallWareApplicationTests {
    @Autowired
    PurchaseService purchaseService;

    @Test
    void contextLoads() {
    }

}
