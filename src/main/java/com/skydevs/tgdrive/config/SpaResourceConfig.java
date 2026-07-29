package com.skydevs.tgdrive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.time.Duration;

/**
 * SPA 单页应用资源处理：对于非 /api/、/webdav/、/uploads/、/ws/ 的前端路由，
 * 统一回退到 index.html，由前端路由器处理。
 * 解决多级路径（如 /user/home）触发 NoResourceFoundException 的问题。
 */
@Configuration
public class SpaResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 带内容 hash 的构建产物（/assets/**）可安全长缓存 1 年，二次访问命中浏览器缓存
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .resourceChain(true);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                // index.html 及无 hash 资源不缓存，保证发布后立即生效
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        // 如果找到真实静态资源（文件/图片等），直接返回
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        // 纵深防御：形似静态资源的请求（带文件扩展名，如 .js/.css/.png/.json/.map）
                        // 若未命中真实文件，返回 null（最终 404），绝不回退到 index.html。
                        // 否则浏览器会把 text/html 当作 JS 模块加载，触发严格 MIME 校验失败而白屏
                        // （例如子路由下相对路径被解析到 /user/assets/xxx.js 时）。
                        if (looksLikeStaticAsset(resourcePath)) {
                            return null;
                        }
                        // 仅对「无扩展名的前端路由」回退到 index.html（SPA 路由交给前端处理）
                        return new ClassPathResource("/static/index.html");
                    }

                    private boolean looksLikeStaticAsset(String path) {
                        int lastSlash = path.lastIndexOf('/');
                        String lastSegment = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
                        // 最后一段包含 '.' 视为带扩展名的资源文件（前端路由通常无扩展名）
                        return lastSegment.contains(".");
                    }
                });
    }
}
