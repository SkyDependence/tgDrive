package com.skydevs.tgdrive.config;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UploadProgressWebSocketHandler uploadProgressWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadProgressWebSocketHandler, "/ws/upload-progress")
                // 握手前校验 Sa-Token 登录态，拒绝未登录连接，防止匿名窃听上传进度
                .addInterceptors(new SaTokenHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    /**
     * WebSocket 握手拦截器：校验登录态并将 userId 透传到会话 attributes
     */
    static class SaTokenHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // 未登录直接拒绝握手
            if (!StpUtil.isLogin()) {
                return false;
            }
            // 将 userId 存入会话属性，供后续按用户隔离广播
            attributes.put("userId", StpUtil.getLoginIdAsLong());
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // 无需后置处理
        }
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