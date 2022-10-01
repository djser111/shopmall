package com.atguigu.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    private Long id;        //库存工作单
    private StockDetailTo detail;      //详情id
}
