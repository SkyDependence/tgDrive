package com.skydevs.tgdrive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 直接使用 Argon2 作为唯一的密码编码器
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
