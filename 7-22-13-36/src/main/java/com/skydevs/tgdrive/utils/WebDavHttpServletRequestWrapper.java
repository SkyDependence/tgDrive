package com.skydevs.tgdrive.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class WebDavHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public WebDavHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getMethod() {
        // 原本是MKCOL等，统一改为POST
        return "POST";
    }

    @Override
    public String getHeader(String name) {
        if ("X-HTTP-Method-Override".equalsIgnoreCase(name)) {
            // 返回真正的WebDav方法
            HttpServletRequest request = (HttpServletRequest) super.getRequest();
            return request.getMethod();
        }

        return super.getHeader(name);
    }
}
