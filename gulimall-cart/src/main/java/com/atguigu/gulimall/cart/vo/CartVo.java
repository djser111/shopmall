package com.atguigu.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CartVo {
    private List<CartItemVo> items;
    private Integer countNum = 0;
    private Integer countType;
    private BigDecimal totalAmount = new BigDecimal("0");
    private BigDecimal reduce = new BigDecimal("0");

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    public void setCountNum() {
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                this.countNum += item.getCount();
            }
        }
    }

    public void setCountType() {
        if (items != null && items.size() > 0) {
            this.countType = items.size();
        }else {
            this.countType = 0;
        }

    }

    public void setTotalAmount() {
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                if (item.getCheck()){
                    totalAmount = totalAmount.add(item.getTotalPrice());
                }
            }
            totalAmount = totalAmount.subtract(reduce);
        }
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }

    public Integer getCountNum() {
        return countNum;
    }

    public Integer getCountType() {
        return countType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
