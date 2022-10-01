package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class MyMQConfig {
    /**
     * 容器中的binding，queue，exchange会自动创建（rabbitmq中没有）
     *
     * @return
     */

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue stockDelayQueue() {
        /*
        死信队列
        x-dead-letter-exchange: stock-event-exchange
        x-dead-letter-routing-key: stock-release
        x-message-ttl: 120000
         */
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "stock-event-exchange");
        arguments.put("x-dead-letter-routing-key", "stock.release");
        arguments.put("x-message-ttl", 120000);
//        String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments
        return new Queue("stock.delay.queue", true, false, false, arguments);
    }

    @Bean
    public Queue stockReleaseStockQueue() {
        /*
        普通队列
         */
        return new Queue("stock.release.stock.queue", true, false, false);
    }

    @Bean
    public Exchange stockEventExchange() {
//        String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange("stock-event-exchange", true, false);
    }

    @Bean
    public Binding stockLockedBinding() {
//        String destination, DestinationType destinationType, String exchange, String routingKey
        return new Binding("stock.delay.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.locked", null);
    }

    @Bean
    public Binding stockReleaseStockBinding() {
        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.release", null);
    }
}
