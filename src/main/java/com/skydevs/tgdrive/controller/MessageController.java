package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.Message;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.TelegramBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description:
 * 消息控制
 * @author SkyDev
 * @date 2025-07-11 17:51:06
 */
@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final TelegramBotService telegramBotService;

    /**
     * Description:
     * 发送消息
     * @param message 消息内容
     * @author SkyDev
     * @date 2025-07-11 17:51:27
     */
    @SaCheckRole("admin")
    @PostMapping("/send-message")
    public Result<String> sendMessage(@Valid @RequestBody Message message) {
        log.info("处理消息发送");
        if (telegramBotService.sendMessage(message.getMessage())) {
            return Result.success("消息发送成功: " + message);
        } else {
            return Result.error("消息发送失败");
        }
    }
}
