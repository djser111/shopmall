package com.atguigu.common.exception;


public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码获取频率太高,请稍后再试"),
    TOO_MANY_REQUEST(10003, "请求发送频发"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户名已存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号已存在"),
    NO_STOCK_EXCEPTION(21000, "商品库存不足"),
    LOGINACCT_PASSWORD_INVAILD_EXCEPTION(15003, "账号或密码错误");

    private final int code;
    private final String message;

    BizCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
