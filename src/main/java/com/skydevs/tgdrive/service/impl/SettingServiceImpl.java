package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.constants.SettingConstant;
import com.skydevs.tgdrive.entity.Setting;
import com.skydevs.tgdrive.exception.setting.SettingNotFoundException;
import com.skydevs.tgdrive.mapper.SettingMapper;
import com.skydevs.tgdrive.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingMapper settingMapper;

    @Override
    public String getSetting(String key) {
        Setting setting = settingMapper.findByKey(key);
        return setting != null ? setting.getValue() : null;
    }

    @Override
    public void updateSetting(Setting setting) {
        // Check if the setting already exists
        if (settingMapper.findByKey(setting.getKey()) != null) {
            settingMapper.update(setting);
        } else {
            throw new SettingNotFoundException();
        }
    }

    @Override
    public List<Setting> getAllSettings() {
        return settingMapper.findAll();
    }

    @Override
    public boolean isRegistrationAllowed() {
        String allowRegistration = this.getSetting(SettingConstant.ALLOW_REGISTRATION);
        return "true".equals(allowRegistration);
    }
}

