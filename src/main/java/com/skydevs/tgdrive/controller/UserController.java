package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.*;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.UserService;
import com.skydevs.tgdrive.service.SettingService;
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
    public Result<UserLogin> login(@RequestBody AuthRequest authRequest) {
        // 验证用户名和密码
        User user = userService.login(authRequest);

        StpUtil.login(user.getId());
        // 将用户角色存储到 session 中
        StpUtil.getSession().set("role", user.getRole());
        
        // 更新最后登录时间
        userService.updateLastLoginTime(user.getId());
        
        UserLogin userLogin = UserLogin.builder()
                .UserId(user.getId())
                .token(StpUtil.getTokenValue())
                .role(user.getRole())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        log.info(user.getId() + "登入");
        return Result.success(userLogin);
    }

    /**
     * 修改密码
     * @param changePasswordRequest 修改密码请求
     * @return 密码修改成功消息
     */
    @PostMapping("change-password")
    public Result<String> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
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
    public Result<UserLogin> register(@RequestBody RegisterRequest registerRequest) {
        // 检查是否允许注册
        String allowRegistration = settingService.getSetting("allow_registration");
        if (!"true".equalsIgnoreCase(allowRegistration)) {
            return Result.error("管理员已关闭注册功能");
        }

        try {
            // 注册用户
            User user = userService.register(registerRequest);
            
            // 自动登录
            StpUtil.login(user.getId());
            StpUtil.getSession().set("role", user.getRole());
            
            UserLogin userLogin = UserLogin.builder()
                    .UserId(user.getId())
                    .token(StpUtil.getTokenValue())
                    .role(user.getRole())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
            
            log.info("用户注册并登录成功: {}", user.getUsername());
            return Result.success(userLogin);
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员修改用户密码
     * @param adminChangePasswordRequest 管理员修改密码请求
     * @return 密码修改成败消息
     */
    @PostMapping("admin/change-password")
    public Result<String> adminChangePassword(@RequestBody AdminChangePasswordRequest adminChangePasswordRequest) {
        // 验证当前用户是否为管理员
        String role = (String) StpUtil.getSession().get("role");
        if (!"admin".equals(role)) {
            return Result.error("权限不足，只有管理员可以修改用户密码");
        }

        try {
            userService.adminChangePassword(adminChangePasswordRequest);
            log.info("管理员修改用户密码成功: {}", adminChangePasswordRequest.getUsername());
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("管理员修改用户密码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有用户列表（管理员功能）
     * @return 用户列表
     */
    @GetMapping("/admin/users")
    public Result<List<User>> getAllUsers() {
        // 验证当前用户是否为管理员
        String role = (String) StpUtil.getSession().get("role");
        if (!"admin".equals(role)) {
            return Result.error("权限不足，只有管理员可以查看用户列表");
        }

        try {
            List<User> users = userService.getAllUsers();
            // 清除密码信息，避免泄露
            users.forEach(user -> user.setPassword(null));
            return Result.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage());
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户（管理员功能）
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/admin/users/{userId}")
    public Result<String> deleteUser(@PathVariable Long userId) {
        // 验证当前用户是否为管理员
        String role = (String) StpUtil.getSession().get("role");
        if (!"admin".equals(role)) {
            return Result.error("权限不足，只有管理员可以删除用户");
        }

        try {
            userService.deleteUser(userId);
            log.info("管理员删除用户成功: {}", userId);
            return Result.success("用户删除成功");
        } catch (Exception e) {
            log.error("管理员删除用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
