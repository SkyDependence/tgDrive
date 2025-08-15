package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.Setting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettingMapper {
    Setting findByKey(@Param("key") String key);

    List<Setting> findAll();

    void update(Setting setting);

    void insert(Setting setting);
}
