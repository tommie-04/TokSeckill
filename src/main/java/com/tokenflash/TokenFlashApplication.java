package com.tokenflash;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

@MapperScan({"com.tokenflash.ai.mapper", "com.tokenflash.community.mapper", "com.tokenflash.provider.mapper", "com.tokenflash.seckill.mapper", "com.tokenflash.tokenpack.mapper", "com.tokenflash.user.mapper"})
@EnableRetry
@EnableKafka
@SpringBootApplication
public class TokenFlashApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenFlashApplication.class, args);
    }

}
