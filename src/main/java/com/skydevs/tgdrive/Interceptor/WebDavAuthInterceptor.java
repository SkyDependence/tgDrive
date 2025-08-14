package com.skydevs.tgdrive.Interceptor;

import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.mapper.UserMapper;
import com.skydevs.tgdrive.service.WebDavConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebDavAuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    
    private final WebDavConfigService webDavConfigService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查WebDAV是否启用
        if (!webDavConfigService.isWebDavEnabled()) {
            log.info("WebDAV服务未启用");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return false;
        }
        
        // 读取 Authorization 头部信息
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.info("WebDAV请求缺少认证信息");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"WebDAV\"");
            return false;
        }

        try {
            // 解码Base64验证
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

            String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                log.info("WebDAV认证格式错误");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"WebDAV\"");
                return false;
            }
            
            String username = values[0];
            String password = values[1];

            User user = userMapper.getUserByUsername(username);
            if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
                log.info("WebDAV用户名或密码错误: {}", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"WebDAV\"");
                return false;
            }
            
            // 检查用户是否有WebDAV访问权限
            if (!webDavConfigService.hasWebDavPermission(user.getRole())) {
                log.info("用户 {} 没有WebDAV访问权限，角色: {}", username, user.getRole());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            // 将用户信息存储到请求属性中，供后续使用
            request.setAttribute("webdav.user", user);
            log.debug("WebDAV用户认证成功: {}", username);
            
            return true;
        } catch (Exception e) {
            log.error("WebDAV认证过程中发生错误: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }
}
