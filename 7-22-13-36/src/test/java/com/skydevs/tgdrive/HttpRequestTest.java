package com.skydevs.tgdrive;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
public class HttpRequestTest {


    @Test
    void testPort() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https"); // 模拟代理协议
        when(request.getHeader("Host")).thenReturn("example.com:443");   // 模拟 Host 头
        when(request.getHeader("X-Forwarded-Port")).thenReturn("443");   // 模拟代理端口
        when(request.getScheme()).thenReturn("http");                    // 默认协议
        when(request.getServerName()).thenReturn("localhost");           // 默认主机名
        when(request.getServerPort()).thenReturn(8080);                  // 默认端口

        String protocol = request.getHeader("X-Forwarded-Proto") != null ?
                request.getHeader("X-Forwarded-Proto") :
                request.getScheme(); // 先代理请求头中获取协议
        String host = request.getHeader("Host") != null ?
                request.getHeader("Host").split(":")[0] : // 去除 Host 头中的端口
                request.getServerName(); // 获取主机名 localhost 或实际域名
        int port = request.getHeader("X-Forwarded-Port") != null ?
                Integer.parseInt(request.getHeader("X-Forwarded-Port")) :
                request.getServerPort(); // 先从代理请求头中获取端口号 80 或其他
        // 如果是默认端口，则省略端口号
        if ((protocol.equalsIgnoreCase("http") && port == 80) || (protocol.equalsIgnoreCase("https") && port == 443)) {
            System.out.println(protocol + "://" + host);
        }
        System.out.println(protocol + "://" + host + ":" + port);
    }
}
