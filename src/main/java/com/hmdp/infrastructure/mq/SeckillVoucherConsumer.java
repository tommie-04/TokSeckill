package com.hmdp.infrastructure.mq;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Lets the container commit the offset after the database transaction has returned successfully. */
@Component
public class SeckillVoucherConsumer {

    private final IVoucherOrderService voucherOrderService;

    public SeckillVoucherConsumer(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    @KafkaListener(topics = "${hmdp.kafka.order-topic}")
    public void processOrder(VoucherOrder order) {
        voucherOrderService.createVoucherOrder(order);
    }
}
