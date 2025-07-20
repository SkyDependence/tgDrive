package com.skydevs.tgdrive.Filter;

import com.skydevs.tgdrive.utils.WebDavHttpServletRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 拦截WebDav方法，将其转化为Spring可识别的处理逻辑
 */
@Component
@Slf4j
public class WebDavFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request =  (HttpServletRequest) servletRequest;
        String method = request.getMethod();

        // 拦截 PROPFIND MKCOL MOVE等WebDav方法
        if ("PROPFIND".equalsIgnoreCase(method)
        || "MKCOL".equalsIgnoreCase(method)
        || "MOVE".equalsIgnoreCase(method)
        || "COPY".equalsIgnoreCase(method)) {
            log.info("拦截到WebDAV请求: {}", method);

            // 把原始方法放到attribute
            request.setAttribute("X-HTTP-Method-Override", method);

            // 包装request，强制返回POST
            HttpServletRequest wrapper = new WebDavHttpServletRequestWrapper(request);

            String uri = request.getRequestURI();
            String forwardPath = "/webdav/dispatch" + uri.substring("/webdav".length());
            log.info("Forward To: {}", forwardPath);

            request.getRequestDispatcher(forwardPath).forward(wrapper, servletResponse);
        } else {
            filterChain.doFilter(request, servletResponse);
        }
    }
}
