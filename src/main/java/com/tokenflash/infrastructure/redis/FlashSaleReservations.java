package com.tokenflash.infrastructure.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

@Component
public class FlashSaleReservations {

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT =
            new DefaultRedisScript<>();

    static {
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate redis;

    public Long reserve(Long packId, Long userId) {
        return redis.execute(SECKILL_SCRIPT,
                List.of(SECKILL_STOCK_KEY + packId, SECKILL_ORDER_KEY + packId, SECKILL_WINDOW_KEY + packId),
                userId.toString(), Long.toString(Instant.now().getEpochSecond()));
    }
}
