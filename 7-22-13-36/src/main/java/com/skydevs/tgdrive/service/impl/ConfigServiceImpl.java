package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.NoConfigException;
import com.skydevs.tgdrive.mapper.ConfigMapper;
import com.skydevs.tgdrive.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    ConfigMapper configMapper;

    /**
     * 根据文件名查询配置
     * @param filename
     * @return
     */
    @Override
    public ConfigForm get(String filename) {
        return configMapper.getByName(filename);
    }

    /**
     * 插入配置
     * @param configForm
     */
    @Override
    public void save(ConfigForm configForm) {
        ConfigForm temp = configMapper.getByName(configForm.getName());
        if (temp == null) {
            configMapper.insert(configForm);
        } else {
            configMapper.deleteByName(configForm.getName());
            configMapper.insert(configForm);
        }
    }

    /**
     * 获取所有配置文件
     * @return
     */
    @Override
    public void delete(String name) {
        configMapper.deleteByName(name);
        log.info("配置删除成功: {}", name);
    }

    @Override
    public List<ConfigForm> getForms() {
        List<ConfigForm> configForms =  configMapper.getAll();
        if (configForms == null) {
            throw new NoConfigException();
        }
        return configForms;
    }
}
