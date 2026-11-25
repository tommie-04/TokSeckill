package com.hmdp.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.hmdp.ai.service.ConsultantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

@Profile("consultant-local")
@RestController
public class ChatController {

    @Resource
    private ConsultantService consultantService;

    @Value("${hmdp.ai.api-key:}")
    private String apiKey;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message) {
        if (StrUtil.isBlank(apiKey)) {
            return Flux.just("请先在 application.yaml 中配置 hmdp.ai.api-key（支持 DeepSeek / 通义千问等 OpenAI 兼容接口）");
        }
        return consultantService.chat(memoryId, message);
    }
}
