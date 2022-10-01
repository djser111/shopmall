package com.atguigu.gulimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 本地事务失效问题
 * 同一个对象内事务方法互调默认失效，原因：绕过了代理对象，事务使用代理对象来控制
 * 解决：使用代理对象来调用事务方法
 *      1）引入aop-starter；spring-boot-starter-aop；引入aspectj
 *      2）@EnableAspectJAutoProxy（exposeProxy = true）；开启aspectj动态代理功能，以后所有的动态代理都是aspectj帮忙创建的（即使没有接口也可以创建动态代理）
 *              exposeProxy = true：对外暴露代理对象
 *      3）用代理对象实现本类互调
 *
 * Seata控制分布式事务
 * 1）每一个微服务先必须创建undo_log；
 * 2）安装事务协调器；seata-server
 * 3）整合
 *      1、导入依赖 spring-cloud-starter-alibaba-seata
 *      2、解压并启动seata-server
 *              registry.conf 注册中心相关的配置； 修改registry type=nacos
 *              file.conf
 *      3、所有想用到分布式事务的微服务使用seata DataSourceProxy代理自己的数据源
 *      4、每个微服务都需要导入registry.conf、file.conf
 */


@SpringBootApplication
@EnableDiscoveryClient
@EnableRabbit
@MapperScan("com.atguigu.gulimall.order.dao")
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.order.feign")
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
