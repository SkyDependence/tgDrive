package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.constants.SettingConstant;
import com.skydevs.tgdrive.entity.Setting;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/setting")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;

    /**
     * Description:
     * 获取注册状态
     * @author SkyDev
     * @date 2025-08-15 14:20:51
     * @return 注册状态
     */
    @GetMapping("/registration-status")
    public Result<Map<String, Boolean>> getRegistrationStatus() {
        String allowRegistration = settingService.getSetting(SettingConstant.ALLOW_REGISTRATION);
        boolean isAllowed = "true".equalsIgnoreCase(allowRegistration);
        return Result.success(Map.of("isRegistrationAllowed", isAllowed));
    }

    /**
     * Description:
     * 获取所有设置
     * @author SkyDev
     * @date 2025-08-15 14:21:04
     * @return 设置列表
     */
    @SaCheckRole("admin")
    @GetMapping("/settings")
    public Result<List<Setting>> getAllSettings() {
        List<Setting> settings = settingService.getAllSettings();
        return Result.success(settings);
    }

    /**
     * Description:
     * 更新设置
     * @author SkyDev
     * @date 2025-08-15 14:21:15
     * @param setting 设置信息
     * @return 成功消息
     */
    @SaCheckRole("admin")
    @PostMapping()
    public Result<String> updateSetting(@Valid @RequestBody Setting setting) {
        settingService.updateSetting(setting);
        log.info("设置更新成功: {} = {}", setting.getKey(), setting.getValue());
        return Result.success("设置更新成功");
    }
}
