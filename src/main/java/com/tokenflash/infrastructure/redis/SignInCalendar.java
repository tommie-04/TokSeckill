package com.tokenflash.infrastructure.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

@Component
public class SignInCalendar {
    @Resource
    private StringRedisTemplate redis;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern(":yyyyMM");

    public void mark(Long userId, LocalDate day) {
        redis.opsForValue().setBit(USER_SIGN_KEY + userId + day.format(MONTH), day.getDayOfMonth() - 1, true);
    }

    public int streak(Long userId, LocalDate day) {
        List<Long> bits = redis.opsForValue().bitField(USER_SIGN_KEY + userId + day.format(MONTH),
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day.getDayOfMonth())).valueAt(0));
        return Long.numberOfTrailingZeros(~bits.get(0));
    }
}
