package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.ChunkUploadResponse;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.dto.UploadPrepareResponse;
import com.skydevs.tgdrive.dto.UploadTaskDTO;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ResumableUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 断点续传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/resumable")
@RequiredArgsConstructor
public class ResumableUploadController {

    private final ResumableUploadService resumableUploadService;

    // 分块大小：10MB
    private static final int MIN_SIZE = 10 * 1024 * 1024;

    /**
     * Description:
     * 准备上传 - 检查文件状态，断点续传
     * @author SkyDev
     * @date 2025-09-29 16:23:41
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileHash 文件哈希值
     * @param request 请求
     * @return 上传准备相应
     */
    @SaCheckLogin
    @PostMapping("/prepare")
    public Result<UploadPrepareResponse> prepareUpload(
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam String fileHash,
            HttpServletRequest request) {

        if (fileSize <= MIN_SIZE) {
            return Result.error("文件大小不足10MB，不需要断点续传");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("准备上传 - 用户: {}, 文件: {}, 大小: {}, Hash: {}", userId, fileName, fileSize, fileHash);

        try {
            UploadPrepareResponse response = resumableUploadService.prepareUpload(fileName, fileSize, fileHash, userId, request);

            if (response.isResumable()) {
                log.info("发现未完成的上传任务，支持续传: {}, 已上传 {}/{} 分块",
                    fileName, response.getUploadedChunks().size(), response.getTotalChunks());
            } else {
                log.info("创建新的上传任务: {}, 总分块数: {}", fileName, response.getTotalChunks());
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("准备上传失败", e);
            return Result.error("准备上传失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 上传文件分块
     * @author SkyDev
     * @date 2025-09-29 16:22:36
     * @param taskId 任务ID
     * @param chunkIndex 分块索引
     * @param chunk 分块
     * @param chunkHash 分块哈希值
     * @return 分块上传完成信息
     */
    @SaCheckLogin
    @PostMapping("/chunk")
    public Result<ChunkUploadResponse> uploadChunk(
            @RequestParam String taskId,
            @RequestParam Integer chunkIndex,
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam(required = false) String chunkHash) {

        log.info("上传分块 - 任务: {}, 分块索引: {}, 大小: {}", taskId, chunkIndex, chunk.getSize());

        try {
            ChunkUploadResponse response = resumableUploadService.uploadChunk(taskId, chunkIndex, chunk, chunkHash);

            if (response.isSuccess()) {
                log.info("分块上传成功 - 任务: {}, 分块: {}, 进度: {}%",
                    taskId, chunkIndex, response.getProgressPercentage());
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("分块上传失败 - 任务: {}, 分块: {}", taskId, chunkIndex, e);
            return Result.error("分块上传失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 完成上传 - 合并所有分块并创建最终文件
     * @author SkyDev
     * @date 2025-09-29 16:14:59
     * @param taskId 上传任务ID
     * @param request 请求
     * @return 上传完成文件信息
     */
    @SaCheckLogin
    @PostMapping("/complete")
    public Result<UploadFile> completeUpload(@RequestParam String taskId, HttpServletRequest request) {
        log.info("完成上传 - 任务: {}", taskId);

        try {
            UploadFile uploadFile = resumableUploadService.completeUpload(taskId, request);
            log.info("上传任务完成 - 任务: {}, 文件: {}", taskId, uploadFile.getFileName());
            return Result.success(uploadFile);
        } catch (Exception e) {
            log.error("完成上传失败 - 任务: {}", taskId, e);
            return Result.error("完成上传失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 取消上传任务
     * @author SkyDev
     * @date 2025-09-29 16:15:58
     * @param taskId 任务ID
     * @return 删除任务响应
     */
    @SaCheckLogin
    @DeleteMapping("/cancel/{taskId}")
    public Result<String> cancelUpload(@PathVariable String taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("取消上传 - 任务: {}, 用户: {}", taskId, userId);

        try {
            resumableUploadService.cancelUpload(taskId, userId);
            return Result.success("上传任务已取消");
        } catch (Exception e) {
            log.error("取消上传失败 - 任务: {}", taskId, e);
            return Result.error("取消上传失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 获取用户的上传任务列表
     * @author SkyDev
     * @date 2025-09-29 16:18:25
     * @return 用户的上传任务列表
     */
    @SaCheckLogin
    @GetMapping("/tasks")
    public Result<List<UploadTaskDTO>> getUploadTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("获取上传任务列表 - 用户: {}", userId);

        try {
            List<UploadTaskDTO> tasks = resumableUploadService.getUserUploadTasks(userId);
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("获取上传任务列表失败", e);
            return Result.error("获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 恢复上传任务
     * @author SkyDev
     * @date 2025-09-29 16:19:50
     * @param taskId 任务ID
     * @return 上传准备信息
     */
    @SaCheckLogin
    @PostMapping("/resume/{taskId}")
    public Result<UploadPrepareResponse> resumeUpload(@PathVariable String taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("恢复上传任务 - 任务: {}, 用户: {}", taskId, userId);

        try {
            UploadPrepareResponse response = resumableUploadService.resumeTask(taskId, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("恢复上传任务失败", e);
            return Result.error("恢复任务失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 批量删除上传任务
     * @author SkyDev
     * @date 2025-09-29 16:20:50
     * @param taskIds 需要删除的任务ID列表
     * @return 统一返回信息
     */
    @SaCheckLogin
    @DeleteMapping("/tasks")
    public Result<String> deleteTasks(@RequestParam List<String> taskIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("批量删除上传任务 - 任务: {}, 用户: {}", taskIds, userId);

        try {
            resumableUploadService.deleteTasks(taskIds, userId);
            return Result.success("任务删除成功");
        } catch (Exception e) {
            log.error("批量删除任务失败", e);
            return Result.error("删除任务失败: " + e.getMessage());
        }
    }


    /**
     * Description:
     * 每天凌晨2点定时清理过期任务
     * @author SkyDev
     * @date 2025-09-29 16:21:46
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredTasks() {
        log.info("开始清理过期的上传任务");
        try {
            resumableUploadService.cleanExpiredTasks();
            log.info("清理过期任务完成");
        } catch (Exception e) {
            log.error("清理过期任务失败", e);
        }
    }
}