package com.skydevs.tgdrive.config;

import com.skydevs.tgdrive.Interceptor.WebDavAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final WebDavAuthInterceptor webDavAuthInterceptor;

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    /**
     * 跨域允许的来源，可通过配置 app.cors.allowed-origins 收紧为具体域名（生产环境建议配置）
     * 默认 "*" 保持兼容；前后端同源部署时建议配置为具体来源
     */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webDavAuthInterceptor)
                .addPathPatterns("/webdav/**");
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的访问路径
        String absoluteUploadPath = Paths.get(uploadPath).toAbsolutePath().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/");
        // SPA 静态资源与路由回退由 SpaResourceConfig 统一处理
    }
}
