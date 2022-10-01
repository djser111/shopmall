package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderRespVo {
    private OrderEntity order;
    private Integer code;       //0->success
}
