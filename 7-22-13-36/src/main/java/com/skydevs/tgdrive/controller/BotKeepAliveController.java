package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.BotKeepAliveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 机器人保活控制器
 * 提供机器人保活功能的管理接口
 * 
 * @author TG-Drive
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/bot/keep-alive")
@RequiredArgsConstructor
public class BotKeepAliveController {

    private final BotKeepAliveService botKeepAliveService;

    /**
     * 手动发送保活消息
     * @return 发送结果
     */
    @PostMapping("/send")
    public Result<String> sendKeepAliveMessage() {
        try {
            boolean success = botKeepAliveService.sendKeepAliveMessage();
            if (success) {
                log.info("手动发送保活消息成功");
                return Result.success("保活消息发送成功");
            } else {
                log.warn("手动发送保活消息失败");
                return Result.error("保活消息发送失败");
            }
        } catch (Exception e) {
            log.error("手动发送保活消息异常", e);
            return Result.error("发送保活消息时发生异常: " + e.getMessage());
        }
    }

    /**
     * 启动定时保活任务
     * @return 启动结果
     */
    @PostMapping("/start")
    public Result<String> startKeepAliveTask() {
        try {
            botKeepAliveService.startKeepAliveTask();
            log.info("定时保活任务启动成功");
            return Result.success("定时保活任务已启动");
        } catch (Exception e) {
            log.error("启动定时保活任务异常", e);
            return Result.error("启动定时保活任务时发生异常: " + e.getMessage());
        }
    }

    /**
     * 停止定时保活任务
     * @return 停止结果
     */
    @PostMapping("/stop")
    public Result<String> stopKeepAliveTask() {
        try {
            botKeepAliveService.stopKeepAliveTask();
            log.info("定时保活任务停止成功");
            return Result.success("定时保活任务已停止");
        } catch (Exception e) {
            log.error("停止定时保活任务异常", e);
            return Result.error("停止定时保活任务时发生异常: " + e.getMessage());
        }
    }

    /**
     * 获取保活任务状态
     * @return 任务状态
     */
    @GetMapping("/status")
    public Result<String> getKeepAliveTaskStatus() {
        try {
            boolean isRunning = botKeepAliveService.isKeepAliveTaskRunning();
            String status = isRunning ? "运行中" : "已停止";
            log.debug("查询保活任务状态: {}", status);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取保活任务状态异常", e);
            return Result.error("获取任务状态时发生异常: " + e.getMessage());
        }
    }

    /**
     * 获取保活功能配置信息
     * @return 配置信息
     */
    @GetMapping("/config")
    public Result<String> getKeepAliveConfig() {
        try {
            String configInfo = "保活消息发送时间: 每天凌晨2点\n" +
                              "保活消息间隔: 24小时\n" +
                              "当前任务状态: " + (botKeepAliveService.isKeepAliveTaskRunning() ? "运行中" : "已停止");
            return Result.success(configInfo);
        } catch (Exception e) {
            log.error("获取保活配置信息异常", e);
            return Result.error("获取配置信息时发生异常: " + e.getMessage());
        }
    }
}