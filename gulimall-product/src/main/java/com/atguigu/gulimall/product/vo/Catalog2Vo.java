package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catalog2Vo {
    private Long catalog1Id;
    private Long id;
    private String name;
    private List<Catalog3Vo> catalog3List;

}
