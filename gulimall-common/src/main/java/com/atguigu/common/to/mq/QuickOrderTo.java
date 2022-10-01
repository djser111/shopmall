package com.atguigu.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuickOrderTo {
    private String orderSn;                 //订单号
    private Long promotionSessionId;        //场次id
    private Long skuId;                     //商品id
    private BigDecimal seckillPrice;        //秒杀价格
    private Integer num;                    //秒杀数量
    private Long memberId;                  //会员id
    private String userName;                //用户名
}
