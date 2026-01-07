package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.result.Result;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/webdav")
@RequiredArgsConstructor
public class WebDavController {
    private final WebDavFileService webDavFileService;
    private final WebDavService webDavService;
    private final FileMapper fileMapper;

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
    @RequestMapping(value = "/dispatch/**", method = { RequestMethod.POST })
    public void handleWebDav(HttpServletRequest request, HttpServletResponse response) throws IOException {
        webDavService.switchMethod(request, response);
    }

    /**
     * 获取指定路径下的直接子目录
     * 
     * @param path 父目录路径，默认为根目录 "/"
     * @return 子目录列表
     */
    @GetMapping("/folders")
    public Result<List<Map<String, Object>>> listFolders(@RequestParam(defaultValue = "/") String path) {
        try {
            // 确保路径以 / 结尾
            if (!path.endsWith("/")) {
                path = path + "/";
            }

            List<FileInfo> allDirs = fileMapper.getDirectoriesByPathPrefix(path);

            // 过滤出直接子目录（不包含更深层级）
            final String parentPath = path;
            List<Map<String, Object>> directChildren = allDirs.stream()
                    .filter(dir -> {
                        String dirPath = dir.getWebdavPath();
                        if (dirPath.equals(parentPath))
                            return false; // 排除自身
                        String relativePath = dirPath.substring(parentPath.length());
                        // 直接子目录：相对路径中只有一个 / 或没有 /
                        int slashCount = relativePath.length() - relativePath.replace("/", "").length();
                        return slashCount <= 1;
                    })
                    .map(dir -> {
                        String dirPath = dir.getWebdavPath();
                        String name = dir.getFileName();
                        return Map.<String, Object>of(
                                "path", dirPath,
                                "name", name,
                                "hasChildren", hasSubDirectories(dirPath, allDirs));
                    })
                    .collect(Collectors.toList());

            return Result.success(directChildren);
        } catch (Exception e) {
            log.error("获取目录列表失败: {}", e.getMessage(), e);
            return Result.error("获取目录列表失败");
        }
    }

    /**
     * 模糊搜索目录
     * 
     * @param keyword 搜索关键词
     * @return 匹配的目录列表
     */
    @GetMapping("/folders/search")
    public Result<List<Map<String, Object>>> searchFolders(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.success(new ArrayList<>());
            }

            List<FileInfo> dirs = fileMapper.searchDirectories(keyword.trim());

            List<Map<String, Object>> results = dirs.stream()
                    .map(dir -> Map.<String, Object>of(
                            "path", dir.getWebdavPath(),
                            "name", dir.getFileName()))
                    .collect(Collectors.toList());

            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索目录失败: {}", e.getMessage(), e);
            return Result.error("搜索目录失败");
        }
    }

    /**
     * 检查目录是否有子目录
     */
    private boolean hasSubDirectories(String path, List<FileInfo> allDirs) {
        String prefix = path.endsWith("/") ? path : path + "/";
        return allDirs.stream()
                .anyMatch(dir -> dir.getWebdavPath().startsWith(prefix) && !dir.getWebdavPath().equals(path));
    }
}
