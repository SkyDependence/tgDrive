package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.AdminChangePasswordRequest;
import com.skydevs.tgdrive.dto.AuthRequest;
import com.skydevs.tgdrive.dto.ChangePasswordRequest;
import com.skydevs.tgdrive.dto.RegisterRequest;
import com.skydevs.tgdrive.entity.User;
import java.util.List;

public interface UserService {

    /**
     * 用户登入
     * @param authRequest 请求参数（用户名，密码）
     * @return User
     */
    User login(AuthRequest authRequest);

    /**
     * 修改密码
     * @param changePasswordRequest 新老密码
     */
    void changePassword(long id, ChangePasswordRequest changePasswordRequest);

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return User
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱获取用户
     * @param email 邮箱
     * @return User
     */
    User getUserByEmail(String email);

    /**
     * 根据用户名或邮箱获取用户
     * @param usernameOrEmail 用户名或邮箱
     * @return User
     */
    User getUserByUsernameOrEmail(String usernameOrEmail);

    /**
     * 用户注册
     * @param registerRequest 注册请求参数
     * @return User
     */
    User register(RegisterRequest registerRequest);

    /**
     * 管理员修改用户密码
     * @param adminChangePasswordRequest 管理员修改密码请求
     */
    void adminChangePassword(AdminChangePasswordRequest adminChangePasswordRequest);

    /**
     * 获取所有用户列表（管理员功能）
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 根据用户ID删除用户（管理员功能）
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 更新用户最后登录时间
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);

    User getById(long userId);
}
