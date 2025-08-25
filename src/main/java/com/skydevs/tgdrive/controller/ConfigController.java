package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.config.ConfigFileNotFoundException;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.utils.StringUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final TelegramBotService telegramBotService;

    /**
     * Description:
     * 获取配置信息
     * @author SkyDev
     * @date 2025-08-15 14:21:54
     * @param name 配置文件名
     * @return ConfigForm
     */
    @SaCheckRole("admin")
    @GetMapping()
    public Result<ConfigForm> getConfig(@NotBlank(message = "配置名不能为空") @RequestParam String name) {
        ConfigForm config = configService.get(name);
        if (config == null) {
            log.error("配置获取失败，请检查文件名是否错误");
            throw new ConfigFileNotFoundException();
        }
        log.info("获取数据成功");
        return Result.success(config);
    }

    /**
     * Description:
     * 获取所有配置
     * @author SkyDev
     * @date 2025-08-15 14:22:02
     * @return 配置文件列表
     */
    @SaCheckRole("admin")
    @GetMapping("/configs")
    public Result<List<ConfigForm>> getConfigs() {
        List<ConfigForm> configForms = configService.getForms();
        return Result.success(configForms);
    }

    /**
     * Description:
     * 提交配置
     * @author SkyDev
     * @date 2025-08-15 14:22:09
     * @param configForm 配置信息
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @PostMapping()
    public Result<String> submitConfig(@Valid @RequestBody ConfigForm configForm) {
        String name = configForm.getName();
        configForm.setName(name.trim());
        configService.save(configForm);
        log.info("配置保存成功");
        return Result.success("配置保存成功");
    }

    /**
     * Description:
     * 删除配置
     * @author SkyDev
     * @date 2025-07-30 16:46:09
     * @param name 配置文件名
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{name}")
    public Result<String> deleteConfig(@NotBlank(message = "配置名不能为空") @PathVariable("name") String name) {
        configService.delete(name);
        log.info("配置删除成功");
        return Result.success("配置删除成功");
    }

    /**
     * Description:
     * 加载配置
     * @author SkyDev
     * @date 2025-08-15 14:22:26
     * @param name 配置文件名
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @GetMapping("/{name}")
    public Result<String> loadConfig(@NotBlank(message = "配置名不能为空") @PathVariable("name") String name) {
        telegramBotService.initializeBot(name);
        log.info("加载配置成功");
        return Result.success("配置加载成功");
    }
}
