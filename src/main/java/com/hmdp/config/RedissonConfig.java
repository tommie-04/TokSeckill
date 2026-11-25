package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Shares the Spring Redis connection settings with the lock examples. */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redis) {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setDatabase(redis.getDatabase());
        if (StringUtils.hasText(redis.getPassword())) {
            server.setPassword(redis.getPassword());
        }
        return Redisson.create(config);
    }
}
