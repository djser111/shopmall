package com.atguigu.common.enume;

import javax.naming.ldap.PagedResultsControl;

public enum OrderStatusEnum {
    CREATE_NEW(0, "待付款"),
    PAYED(1, "已付款"),
    SENDED(2, "已发货"),
    RECEIVED(3, "已完成"),
    CANCLED(4, "已取消"),
    SERVICING(5, "售后中"),
    SERVICED(6, "售后完成");
    private Integer code;
    private String message;

    OrderStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
