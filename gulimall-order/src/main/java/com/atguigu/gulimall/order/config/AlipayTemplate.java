package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000121671711";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC03XfI36cJxCICsU4sasMIUX3VV/wsI5v0w3utO2M9uYsnmEw3rS3xkXZcpLXsvFFn97gS2scYsr/JjOq/hwWY6R/SdN21zgqzzYjwBtdQc3oQNxPKwqlzf4Oc36xqI98iOK9jIIMSXUfTKrcP4NUTZL7WlR0AdRj6o3uI+//8qHrFeGB+1UiWPbJbncAFNGSRVtIB8FBjH7hNo/G3+OHdzNZdPKeXfCdyjInawxGpkhHu3lmtg05srE3T8L7tQtcJBjqzNXPZCDrxLbOpx5lQh4jJYsUZY0W9B/nV6/C82uIbLEqirwmgIAf+2KSpH5oSSXi3LRqDjQZtYF08FX31AgMBAAECggEALNF0jWJekhz4EJK/PGJ4Uedfty5GXum0C8MlMpg0z2bbBMTInkNbzzCsk+h87Fz+DzVjd8ie7Y/d1qQEx9E9odai/BPZnOOh40xbUp9fW6BB7yK21NfIBcbjZkxG/UZGVMSXMYks8bALzFgZZOXh5xxf7eylcyeROQqp4hgQJxttbMRUI0X8Ti3q7av25v+xtFg6ZxVdVLFoZk95/Di1XOZGTxyRnEQc1T7zaNBcP79XN7/jKXUaSvKoUe+QAi5qIFKU5X8RvGsK6EO9Jp9+n0B+tmizYTJ07ylPHKlvMumJqDW3AZg9F3R7sKhGb6/ciesE8GudDaxw3+gj0T8vQQKBgQDyNqdE1CWS2WTEziehjN8X+5hvn3C8k/LGKZftMng7q9BnXIK7J0tJ0wnfXNvQTIYBr0rbrQkO9KFiGoxtMtq5jVYARGwHlRhdPvcoKWAx9JZz/IHcsbeu+5MwHO+4jnVkTntFlsvxcYAeeMmfqIQ+runXDhhISGicP0J9q6W9bwKBgQC/KOTER6hm+vvXreOCTRmjiTp+pNXEE4vTP6qoRymcBKfj8k32o84bBDYbnV76fSXul3zfdmJ6/cP/Eo1mYedKZcwkXNEgq+artIEdn7tPKoVqsof5Jb/r+ryybK5ZiAxCwNjh34JDkXgDKzK49amKZgdfps1705VuRe9oHiKQ2wKBgDQ+haXa/J/INGwe6311HUnXAvJQuchzQRJtNk/7auO0E6e31Jr5xsuNsbt0FBXB68XBQaxQjnujWIwInfGP3o3XZo14NLUN+8thIX6QLieYUjuCY6Bu1Ofxa7YdB7gPQlL4eq5v8F2L1c68zwCZIK1EnTu91o8Az0+kdXsV4xIRAoGAa9r14G0R7jC0QcfB8vXvfl7iGyyD1CK3JNTqRBIKvxW9aJaBOKTJmGKy6LfNLAXKjij48thHzl8548Qi9d5NcqOnH+kisY1sE/s/UmiadZtnNYFPyNpsxAdvyjgZ0zg4ur02YZMLW3ZLQXZxIvyw0P6qMGTb0X7a9CIPZy6BkDsCgYBgauqZPwH8z03vvd3ABRpMRWFuOKI8X7YPHtM7TkUsFmbW3A/oShBnPrN7yRYrim9g0v5rk8RevReoBgzmd9gD7c8gECHuTg5UeJxg+kZAKxpH+8lL2IvnoXb2dTlCkRcXep35NPPLwqLtGQj42MrzWAVT0mP/ofIm22qNz1FB+A==";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxwjrWWkKUGG5/p0J3KLaDDTfX2z72oEZXZ78/BhQ+WXvDfJBwBirrGuXFJcOkoKPbmQfS7fN4mfgtCVFm/wl2T8/REpt8smgUZoWGIxb0D05JQUrzG3iyO3S2R8Zx9Vtq3C73yQnyO8OLJcrIfRcwUotxwq0pViTNOtKg5Myqy0z61xMuWadOhUSWliwvGcB4mJUTt/hS36xpFKPcv3dQM5hW2tjb4zmg36vMJAmx2xJALkxMw26Yw7JgEH5LnvN7HwAz1LWQWrrqfXKJ+VoejySC+iuopKvGsHr6ZKTI29kgktgQHqTWyNcKPIjcEGe3Fcsqbdnnvfx4P7ntvzegQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "http://gulimall.free.idcfengye.com/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\"1m\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
