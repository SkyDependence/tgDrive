package com.skydevs.tgdrive.config;

import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UploadProgressWebSocketHandler uploadProgressWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadProgressWebSocketHandler, "/ws/upload-progress")
                .setAllowedOrigins("*"); // 在生产环境中应该限制具体的域名
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置WebSocket超时时间为5分钟（单位：毫秒）
        container.setMaxSessionIdleTimeout(5 * 60 * 1000L);
        // 设置文本消息缓冲区大小
        container.setMaxTextMessageBufferSize(8192);
        // 设置二进制消息缓冲区大小
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }
}