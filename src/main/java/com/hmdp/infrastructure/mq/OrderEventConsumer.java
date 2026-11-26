package com.hmdp.infrastructure.mq;

import com.hmdp.seckill.entity.TokenPackOrder;
import com.hmdp.seckill.service.ITokenPackOrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Lets the container commit the offset after the database transaction has returned successfully. */
@Component
public class OrderEventConsumer {

    private final ITokenPackOrderService orderService;

    public OrderEventConsumer(ITokenPackOrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${hmdp.kafka.order-topic}")
    public void processOrder(TokenPackOrder order) {
        orderService.persistReservedOrder(order);
    }
}
