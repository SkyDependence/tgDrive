package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

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
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return
     */
    @Select("SELECT * FROM users where email = #{email}")
    User getUserByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     * @param usernameOrEmail 用户名或邮箱
     * @return
     */
    @Select("SELECT * FROM users where username = #{usernameOrEmail} OR email = #{usernameOrEmail}")
    User getUserByUsernameOrEmail(String usernameOrEmail);

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

    /**
     * 插入新用户
     * @param user 用户信息
     */
    @Insert("INSERT INTO users (username, password, role, email, reserved_1, reserved_2, reserved_3) VALUES (#{username}, #{password}, #{role}, #{email}, #{reserved1}, #{reserved2}, #{reserved3})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(User user);

    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    @Select("SELECT * FROM users ORDER BY id DESC")
    List<User> getAllUsers();

    /**
     * 根据用户ID删除用户
     * @param userId 用户ID
     */
    @Delete("DELETE FROM users WHERE id = #{userId}")
    void deleteUser(Long userId);

    /**
     * 更新用户最后登录时间
     * @param userId 用户ID
     * @param lastLoginTime 最后登录时间
     */
    @Update("UPDATE users SET reserved_1 = #{lastLoginTime} WHERE id = #{userId}")
    void updateLastLoginTime(Long userId, String lastLoginTime);
}
