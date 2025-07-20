package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.dto.ConfigForm;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConfigMapper {
    /**
     * 插入一个配置
     * @param configForm
     */
    void insert(ConfigForm configForm);


    /**
     * 根据配置名查询配置
     * @param name
     * @return
     */
    @Select("SELECT * From configs where name = #{name}")
    ConfigForm getByName(@Param("name")String name);

    /**
     * 根据名称删除整行
     * @param name
     */
    @Delete("DELETE FROM configs where name = #{name}")
    void deleteByName(String name);

    /**
     * 获取所有配置文件
     * @return
     */
    @Select("SELECT * FROM configs")
    List<ConfigForm> getAll();
}
