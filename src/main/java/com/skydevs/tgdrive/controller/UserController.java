package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.*;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.SettingService;
import com.skydevs.tgdrive.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final SettingService settingService;

    /**
     * 用户登入
     * @param authRequest 用户名、密码
     * @return 登入状态
     */
    @PostMapping("/login")
    public Result<UserLogin> login(@Valid @RequestBody AuthRequest authRequest) {
        // 验证用户名和密码
        User user = userService.login(authRequest);

        StpUtil.login(user.getId());
        // 将用户角色存储到 session 中
        StpUtil.getSession().set("role", user.getRole());
        
        // 更新最后登录时间
        userService.updateLastLoginTime(user.getId());
        
        long tokenTimeoutSeconds = StpUtil.getTokenTimeout();
        Long expireAt = null;
        if (tokenTimeoutSeconds > 0) {
            expireAt = System.currentTimeMillis() + tokenTimeoutSeconds * 1000;
        }

        UserLogin userLogin = UserLogin.builder()
                .UserId(user.getId())
                .token(StpUtil.getTokenValue())
                .role(user.getRole())
                .username(user.getUsername())
                .email(user.getEmail())
                .expireAt(expireAt)
                .build();

        log.info(user.getId() + "登入");
        return Result.success(userLogin);
    }

    /**
     * 修改密码
     * @param changePasswordRequest 修改密码请求
     * @return 密码修改成功消息
     */
    @SaCheckLogin
    @PostMapping("change-password")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        long userId = StpUtil.getLoginIdAsLong();

        userService.changePassword(userId, changePasswordRequest);

        log.info(userId + "密码修改成功");
        return Result.success("密码修改成功");
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<UserLogin> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 检查是否允许注册
        if (!settingService.isRegistrationAllowed()) {
            return Result.error("注册功能已关闭");
        }

        try {
            // 注册用户
            User user = userService.register(registerRequest);

            // 自动登录
            AuthRequest authRequest = AuthRequest.builder()
                            .username(registerRequest.getUsername())
                            .password(registerRequest.getPassword())
                            .build();
            Result<UserLogin> userLogin = login(authRequest);

            log.info("用户注册并登录成功: {}", user.getUsername());
            return userLogin;
        } catch (RuntimeException e) {
            // 业务异常（如用户已存在、密码校验失败等）返回可读消息
            log.warn("用户注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员修改用户密码
     * @param adminChangePasswordRequest 管理员修改密码请求
     * @return 密码修改成败消息
     */
    @SaCheckRole("admin")
    @PostMapping("/admin/change-password")
    public Result<String> adminChangePassword(@Valid @RequestBody AdminChangePasswordRequest adminChangePasswordRequest) {
        userService.adminChangePassword(adminChangePasswordRequest);
        log.info("管理员修改用户密码成功: {}", adminChangePasswordRequest.getUsername());
        return Result.success("密码修改成功");
    }

    /**
     * 获取所有用户列表（管理员功能）
     * @return 用户列表
     */
    @SaCheckRole("admin")
    @GetMapping("/admin/users")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        // 清除密码信息，避免泄露
        users.forEach(user -> user.setPassword(null));
        return Result.success(users);
    }

    /**
     * 删除用户（管理员功能）
     * @param userId 用户ID
     * @return 删除结果
     */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/users/{userId}")
    public Result<String> deleteUser(@NotNull(message = "userID不能为空") @PathVariable Long userId) {
        userService.deleteUser(userId);
        log.info("管理员删除用户成功: {}", userId);
        return Result.success("用户删除成功");
    }
}
