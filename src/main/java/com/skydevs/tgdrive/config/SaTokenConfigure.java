package com.skydevs.tgdrive.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，对 /api/** 强制登录校验（纵深防御，避免漏加注解的接口被匿名访问）
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                // 放行需要匿名访问的接口
                // 注意：/api/file-list 不再放行，文件列表必须登录后按角色过滤，防止未授权枚举文件（含 fileID）
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/setting/registration-status",
                        "/api/setting/visitor-status"
                );
    }
}