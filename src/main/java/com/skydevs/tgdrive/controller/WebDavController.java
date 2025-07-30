package com.skydevs.tgdrive.controller;

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

import java.io.IOException;
import java.io.InputStream;

@RestController
@Slf4j
@RequestMapping("/webdav")
@RequiredArgsConstructor
public class WebDavController {
    private final WebDavFileService webDavFileService;
    private final WebDavService webDacService;

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
        return webDavFileService.downloadByWebDav(request.getRequestURI().substring("/webdav".length()));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/**")
    public void handleDelete(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = StringUtil.getPath(request.getRequestURI());
            webDavFileService.deleteByWebDav(path);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
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
        webDacService.switchMethod(request, response);
    }


}
