package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.AuthRequest;
import com.skydevs.tgdrive.dto.ChangePasswordRequest;
import com.skydevs.tgdrive.dto.UserLogin;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    private UserService userService;

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
        
        UserLogin userLogin = UserLogin.builder()
                .UserId(user.getId())
                .token(StpUtil.getTokenValue())
                .role(user.getRole())
                .build();

        log.info(user.getId() + "登入");
        return Result.success(userLogin);
    }

    /**
     * 修改密码
     * @param changePasswordRequest 修改密码请求
     * @return
     */
    @PostMapping("change-password")
    public Result<String> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        long userId = StpUtil.getLoginIdAsLong();

        userService.changePassword(userId, changePasswordRequest);

        log.info(userId + "密码修改成功");
        return Result.success("密码修改成功");
    }
}
