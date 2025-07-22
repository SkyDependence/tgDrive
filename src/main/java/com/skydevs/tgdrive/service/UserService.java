package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.AuthRequest;
import com.skydevs.tgdrive.dto.ChangePasswordRequest;
import com.skydevs.tgdrive.entity.User;

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
}
