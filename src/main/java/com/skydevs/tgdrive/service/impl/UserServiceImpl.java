package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.AdminChangePasswordRequest;
import com.skydevs.tgdrive.dto.AuthRequest;
import com.skydevs.tgdrive.dto.ChangePasswordRequest;
import com.skydevs.tgdrive.dto.RegisterRequest;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.PasswordErrorException;
import com.skydevs.tgdrive.exception.PasswordValidationException;
import com.skydevs.tgdrive.exception.UserAlreadyExistsException;
import com.skydevs.tgdrive.exception.UserNotFoundException;
import com.skydevs.tgdrive.mapper.UserMapper;
import com.skydevs.tgdrive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户名返回用户
     * @param username 用户名
     * @return User
     */
    @Override
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }

    /**
     * 根据邮箱返回用户
     * @param email 邮箱
     * @return User
     */
    @Override
    public User getUserByEmail(String email) {
        return userMapper.getUserByEmail(email);
    }

    /**
     * 根据用户名或邮箱返回用户
     * @param usernameOrEmail 用户名或邮箱
     * @return User
     */
    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        return userMapper.getUserByUsernameOrEmail(usernameOrEmail);
    }

    /**
     * 用户登入
     * @param authRequest 请求参数（用户名或邮箱，密码）
     * @return
     */
    @Override
    public User login(AuthRequest authRequest) {
        // 支持用户名或邮箱登录
        User user = getUserByUsernameOrEmail(authRequest.getUsername());

        if (user == null) {
            throw new UserNotFoundException();
        }

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new PasswordErrorException();
        }

        return user;
    }

    /**
     * 修改密码
     * @param changePasswordRequest 新老密码
     */
    @Override
    public void changePassword(long id, ChangePasswordRequest changePasswordRequest) {
        // 查找用户
        User user = userMapper.getUserById(id);

        if (user == null) {
            throw new UserNotFoundException();
        }

        // 检查旧密码是否相符
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new PasswordErrorException("原密码错误");
        }

        // 更新密码
        String newPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
        userMapper.updatePassword(id, newPassword);
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求参数
     * @return User
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        // 验证用户名是否已存在
        User existingUser = getUserByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new UserAlreadyExistsException("用户名已存在");
        }

        // 验证邮箱是否已存在
        User existingEmailUser = getUserByEmail(registerRequest.getEmail());
        if (existingEmailUser != null) {
            throw new UserAlreadyExistsException("邮箱已存在");
        }

        // 验证密码强度
        if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 6) {
            throw new PasswordValidationException("密码长度至少为6位");
        }

        // 验证确认密码
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new PasswordValidationException("两次输入的密码不一致");
        }

        // 创建新用户
        String encryptedPassword = passwordEncoder.encode(registerRequest.getPassword());
        User newUser = User.builder()
                .username(registerRequest.getUsername())
                .password(encryptedPassword)
                .role("user") // 新注册用户默认为普通用户
                .email(registerRequest.getEmail())
                .build();

        // 插入数据库
        userMapper.insertUser(newUser);
        
        log.info("用户注册成功: {}", newUser.getUsername());
        return newUser;
    }

    /**
     * 管理员修改用户密码
     * @param adminChangePasswordRequest 管理员修改密码请求
     */
    @Override
    public void adminChangePassword(AdminChangePasswordRequest adminChangePasswordRequest) {
        // 根据用户名查找用户
        User user = getUserByUsername(adminChangePasswordRequest.getUsername());
        if (user == null) {
            throw new UserNotFoundException("用户不存在");
        }

        // 加密新密码
        String newPassword = passwordEncoder.encode(adminChangePasswordRequest.getNewPassword());
        
        // 更新密码
        userMapper.updatePassword(user.getId(), newPassword);
        
        log.info("管理员修改用户密码成功: {}", user.getUsername());
    }

    /**
     * 获取所有用户列表（管理员功能）
     * @return 用户列表
     */
    @Override
    public List<User> getAllUsers() {
        return userMapper.getAllUsers();
    }

    /**
     * 根据用户ID删除用户（管理员功能）
     * @param userId 用户ID
     */
    @Override
    public void deleteUser(Long userId) {
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("用户不存在");
        }
        
        // 不允许删除管理员账户
        if ("admin".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员账户");
        }
        
        userMapper.deleteUser(userId);
        log.info("管理员删除用户成功: {}", user.getUsername());
    }

    /**
     * 更新用户最后登录时间
     * @param userId 用户ID
     */
    @Override
    public void updateLastLoginTime(Long userId) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        userMapper.updateLastLoginTime(userId, currentTime);
    }

    @Override
    public User getById(long userId) {
        return userMapper.getUserById(userId);
    }
}
