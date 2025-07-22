package com.skydevs.tgdrive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private UploadProgressWebSocketHandler uploadProgressWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadProgressWebSocketHandler, "/ws/upload-progress")
                .setAllowedOrigins("*"); // 在生产环境中应该限制具体的域名
    }
}