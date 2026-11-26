package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

@MapperScan({"com.hmdp.mapper", "com.hmdp.seckill.mapper", "com.hmdp.tokenpack.mapper", "com.hmdp.user.mapper"})
@EnableRetry
@EnableKafka
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

}
