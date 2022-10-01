package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SearchParamVo {
    private Long catalog3Id;
    private String keyword;

    //排序规则
    private String sort;
    private Integer hasStock;       //0（代表无库存），1（代表有库存）
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;
    private Integer pageNum = 1;

    private String queryString;
}
