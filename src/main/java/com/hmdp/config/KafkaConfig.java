package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Keeps record retries bounded. Failed records currently go to an error log for investigation. */
@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler orderErrorHandler(
            @Value("${kafka.backoff.interval:1000}") long interval,
            @Value("${kafka.backoff.max_failure:3}") long maxRetries) {
        DefaultErrorHandler handler = new DefaultErrorHandler((record, failure) ->
                log.error("Order event failed: topic={}, partition={}, offset={}, key={}",
                        record.topic(), record.partition(), record.offset(), record.key(), failure),
                new FixedBackOff(interval, maxRetries));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
