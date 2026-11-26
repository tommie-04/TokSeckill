package com.tokenflash.infrastructure.redis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.tokenflash.user.dto.UserDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

@Component
public class UserSessionStore {
    @Resource
    private StringRedisTemplate redis;

    public void saveCode(String phone, String code) {
        redis.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
    }

    public String readCode(String phone) {
        return redis.opsForValue().get(LOGIN_CODE_KEY + phone);
    }

    public void saveSession(String token, UserDTO user) {
        String key = LOGIN_USER_KEY + token;
        Map<String, Object> fields = BeanUtil.beanToMap(user, new HashMap<>(), CopyOptions.create()
                .setIgnoreNullValue(true).setFieldValueEditor((name, value) -> Objects.toString(value, null)));
        redis.opsForHash().putAll(key, fields);
        redis.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
    }

    public UserDTO refreshSession(String token) {
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> fields = redis.opsForHash().entries(key);
        if (fields.isEmpty()) {
            return null;
        }
        redis.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return BeanUtil.fillBeanWithMap(fields, new UserDTO(), false);
    }
}
