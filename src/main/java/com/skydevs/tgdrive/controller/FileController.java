package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    /**
     * 上传文件
     *
     * @param multipartFile 上传文件
     * @return 文件信息
     */
    @SaCheckLogin
    @PostMapping("/upload")
    public CompletableFuture<Result<UploadFile>> uploadFile(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request) {
        // 在主线程中获取用户信息，避免异步线程中无法获取 session
        if (multipartFile == null || multipartFile.isEmpty()) {
            return CompletableFuture.completedFuture(Result.error("上传的文件为空"));
        }
        
        final long userId = StpUtil.getLoginIdAsLong();
        
        return CompletableFuture.supplyAsync(() -> Result.success(fileStorageService.getUploadFile(multipartFile, request, userId)));
    }

    /**
     * 获取文件列表
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    @GetMapping("/fileList")
    public Result<PageResult> getFileList(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) Long userId) {
        Long currentUserId = null;
        String role = "visitor";
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
            role = StpUtil.getSession().getString("role");
        }
        
        // 如果是管理员且指定了userId参数，则按指定用户筛选
        Long filterUserId = currentUserId;
        if ("admin".equals(role) && userId != null) {
            filterUserId = userId;
            role = "admin_filter"; // 特殊角色标识，用于在mapper中处理
        }
        
        PageResult pageResult = fileStorageService.getFileList(page, size, keyword, filterUserId, role);
        return Result.success(pageResult);
    }

    /**
     * 更新文件url
     * @return 成功消息
     */
    @SaCheckLogin
    @PutMapping("/file-url")
    public Result<String> updateFileUrl(HttpServletRequest request) {
        log.info("更新文件url");
        fileStorageService.updateUrl(request);
        return Result.success();
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     * @return 成败消息
     */
    @SaCheckLogin
    @PutMapping("/file/{fileId}/public")
    public Result<String> updateFilePublic(@PathVariable String fileId, @RequestBody Map<String, Boolean> body) {
        boolean isPublic = body.get("isPublic");
        long userId = StpUtil.getLoginIdAsLong();
        String role = StpUtil.getSession().getString("role");
        fileStorageService.updateIsPublic(fileId, isPublic, userId, role);
        return Result.success("更新成功");
    }
    @SaCheckLogin
    @DeleteMapping("/file/{fileId}")
    public Result<String> deleteFile(@PathVariable String fileId) {
        log.info("删除文件，fileId: {}", fileId);
        try {
            long userId = StpUtil.getLoginIdAsLong();
            String role = StpUtil.getSession().getString("role");
            fileStorageService.deleteFile(fileId, userId, role);
            return Result.success("文件删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }
}