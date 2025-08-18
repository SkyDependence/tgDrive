package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.Setting;

import java.util.List;

public interface SettingService {
    /**
     * Description:
     * 根据key获取setting
     * @author SkyDev
     * @date 2025-08-15 11:21:22
     * @param key key
     * @return setting
     */
    String getSetting(String key);

    /**
     * Description:
     * 更新setting
     * @author SkyDev
     * @date 2025-08-15 11:21:50
     * @param setting 设置类
     */
    void updateSetting(Setting setting);

    /**
     * Description:
     * 获取全部setting
     * @author SkyDev
     * @date 2025-08-15 11:22:21
     * @return settings
     */
    List<Setting> getAllSettings();

    /**
     * Description:
     * 检查是否开放注册
     * @author SkyDev
     * @date 2025-08-18 09:25:37
     * @return true为开放，false为关闭
     */
    boolean isRegistrationAllowed();
}
