package com.hmdp.infrastructure.mq;

import com.hmdp.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class OrderEventPublisher {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${hmdp.kafka.order-topic}")
    private String orderTopic;

    public boolean publishAndAwaitAck(VoucherOrder order) {
        try {
            kafkaTemplate.send(orderTopic, order.getId().toString(), order).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for order admission, orderId={}", order.getId(), e);
        } catch (ExecutionException | RuntimeException e) {
            log.error("Kafka publication could not be confirmed, orderId={}", order.getId(), e);
        }
        return false;
    }
}
