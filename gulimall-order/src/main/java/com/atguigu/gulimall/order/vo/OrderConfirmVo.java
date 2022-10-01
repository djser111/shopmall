package com.atguigu.gulimall.order.vo;

import com.atguigu.common.vo.OrderItemVo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    @Getter
    @Setter
    private List<MemberAddressVo> address;

    @Getter
    @Setter
    private List<OrderItemVo> items;

    @Getter
    @Setter
    private Integer integration;

    @Getter
    private BigDecimal total;

    @Getter
    private BigDecimal payPrice;

    @Getter
    @Setter
    private Map<Long, Boolean> stock;

    @Getter
    @Setter
    private String orderToken;

    public Integer getCount() {
        int count = 0;
        if (items != null && items.size() > 0) {
            count = items.size();
        }
        return count;
    }

    public void setTotal() {
        total = new BigDecimal("0");
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                total = total.add(item.getTotalPrice());
            }
        }
    }


    public void setPayPrice() {
        this.payPrice = this.getTotal();
    }
}
