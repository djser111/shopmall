package com.atguigu.gulimall.cart.vo;

import lombok.Data;

@Data
public class UserInfoVo {
    private Long userId;
    private String userKey;
    private boolean tempUser = false;
}
