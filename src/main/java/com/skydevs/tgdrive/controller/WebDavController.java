package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.exception.BaseException;
import com.skydevs.tgdrive.service.WebDavFileService;
import com.skydevs.tgdrive.service.WebDavService;
import com.skydevs.tgdrive.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;

@RestController
@Slf4j
@RequestMapping("/webdav")
@RequiredArgsConstructor
public class WebDavController {
    private final WebDavFileService webDavFileService;
    private final WebDavService webDavService;

    /**
     * 上传文件
     */
    @PutMapping("/**")
    public void handlePut(HttpServletRequest request, HttpServletResponse response) {
        try (InputStream inputStream = request.getInputStream()) {
            webDavFileService.uploadByWebDav(inputStream, request);
            response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/**")
    public ResponseEntity<StreamingResponseBody> handleGet(HttpServletRequest request) {
        // URL 解码：getRequestURI() 返回未解码路径，中文等特殊字符以 %xx 编码
        String path = request.getRequestURI().substring("/webdav".length());
        try {
            path = UriUtils.decode(path, "UTF-8");
        } catch (Exception e) {
            log.warn("WebDAV 下载路径解码失败: {}", path);
        }
        return webDavFileService.downloadByWebDav(path);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/**")
    public void handleDelete(HttpServletRequest request, HttpServletResponse response) {
        try {
            // URL 解码：getRequestURI() 返回未解码路径，中文等特殊字符以 %xx 编码
            String path = StringUtil.getPath(request.getRequestURI());
            try {
                path = UriUtils.decode(path, "UTF-8");
            } catch (Exception e) {
                log.warn("WebDAV 删除路径解码失败: {}", path);
            }
            webDavFileService.deleteByWebDav(path);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
        } catch (BaseException e) {
            // 配置禁用等业务异常返回 403
            log.warn("WebDAV 删除被拒绝: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
        }
    }

    /**
     * 处理探测请求
     */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public void handleOptions(HttpServletResponse response) {
        response.setHeader("Allow", "OPTIONS, HEAD, GET, PUT, DELETE, POST, PROPFIND, MKCOL, MOVE, COPY");
        response.setHeader("DAV", "1,2");
        response.setHeader("MS-Author-Via", "DAV");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * 处理特殊的webdav方法
     */
    @RequestMapping(value = "/dispatch/**", method = {RequestMethod.POST})
    public void handleWebDav(HttpServletRequest request, HttpServletResponse response) throws IOException {
        webDavService.switchMethod(request, response);
    }
}
