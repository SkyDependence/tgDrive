package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.entity.Setting;
import com.skydevs.tgdrive.exception.ConfigFileNotFoundException;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.SettingService;
import com.skydevs.tgdrive.service.TelegramBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final TelegramBotService telegramBotService;
    private final SettingService settingService;

    /**
     * 获取配置文件信息
     * @param name 配置文件名
     * @return ConfigForm
     */
    @SaCheckRole("admin")
    @GetMapping()
    public Result<ConfigForm> getConfig(@RequestParam String name) {
        ConfigForm config = configService.get(name);
        if (config == null) {
            log.error("配置获取失败，请检查文件名是否错误");
            throw new ConfigFileNotFoundException();
        }
        log.info("获取数据成功");
        return Result.success(config);
    }

    /**
     * 获取所有配置文件
     * @return 配置文件列表
     */
    @SaCheckRole("admin")
    @GetMapping("/configs")
    public Result<List<ConfigForm>> getConfigs() {
        List<ConfigForm> configForms = configService.getForms();
        return Result.success(configForms);
    }

    /**
     * 提交配置文件
     * @param configForm 配置信息
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @PostMapping()
    public Result<String> submitConfig(@RequestBody ConfigForm configForm) {
        configService.save(configForm);
        log.info("配置保存成功");
        return Result.success("配置保存成功");
    }

    /**
     * Description:
     * 删除配置文件
     * @author SkyDev
     * @date 2025-07-30 16:46:09
     * @param name 配置文件名
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{name}")
    public Result<String> deleteConfig(@PathVariable("name") String name) {
        configService.delete(name);
        log.info("配置删除成功");
        return Result.success("配置删除成功");
    }

    /**
     * Description:
     * 加载配置
     * @param filename 配置文件名
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @GetMapping("/{filename}")
    public Result<String> loadConfig(@PathVariable("filename") String filename) {
        telegramBotService.initializeBot(filename);
        log.info("加载配置成功");
        return Result.success("配置加载成功");
    }

    /**
     * 获取注册状态
     * @return 注册状态
     */
    @GetMapping("/registration-status")
    public Result<Map<String, Boolean>> getRegistrationStatus() {
        String allowRegistration = settingService.getSetting("allow_registration");
        boolean isAllowed = "true".equalsIgnoreCase(allowRegistration);
        return Result.success(Map.of("isRegistrationAllowed", isAllowed));
    }

    /**
     * 获取所有设置
     * @return 设置列表
     */
    @SaCheckRole("admin")
    @GetMapping("/settings")
    public Result<List<Setting>> getAllSettings() {
        List<Setting> settings = settingService.getAllSettings();
        return Result.success(settings);
    }

    /**
     * 更新设置
     * @param setting 设置信息
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @PostMapping("/settings")
    public Result<String> updateSetting(@Valid @RequestBody Setting setting) {
        settingService.updateSetting(setting);
        log.info("设置更新成功: {} = {}", setting.getKey(), setting.getValue());
        return Result.success("设置更新成功");
    }
}
