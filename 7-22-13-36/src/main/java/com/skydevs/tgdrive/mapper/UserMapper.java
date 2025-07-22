package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {


    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return
     */
    @Select("SELECT * FROM users where username = #{username}")
    User getUserByUsername(String username);

    /**
     * 根据id查找用户
     * @param id 用户id
     * @return User
     */
    @Select("SELECT * FROM users where id = #{id}")
    User getUserById(long id);

    /**
     * 根据id更新密码
     * @param id 用户id
     * @param newPassword 新密码
     */
    @Update("UPDATE users SET password = #{newPassword} where id = #{id}")
    void updatePassword(long id, String newPassword);
}
