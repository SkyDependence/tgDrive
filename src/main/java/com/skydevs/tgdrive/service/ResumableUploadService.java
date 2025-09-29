package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.ChunkUploadResponse;
import com.skydevs.tgdrive.dto.UploadPrepareResponse;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.dto.UploadTaskDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 断点续传服务接口
 */
public interface ResumableUploadService {

    /**
     * Description:
     * 准备上传，检查文件状态
     * @author SkyDev
     * @date 2025-09-29 16:27:01
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileHash 文件哈希值
     * @param userId 用户ID
     * @return 上传准备响应
     */
    UploadPrepareResponse prepareUpload(String fileName, Long fileSize, String fileHash, Long userId, HttpServletRequest request);

    /**
     * Description:
     * 上传文件分块
     * @author SkyDev
     * @date 2025-09-29 16:27:07
     * @param taskId 任务ID
     * @param chunkIndex 分块索引
     * @param chunk 分块文件
     * @param chunkHash 分块哈希（可选，用于验证）
     * @return 分块上传响应
     */
    ChunkUploadResponse uploadChunk(String taskId, Integer chunkIndex, MultipartFile chunk, String chunkHash);

    /**
     * Description:
     * 完成上传任务
     * @author SkyDev
     * @date 2025-09-29 16:27:14
     * @param taskId 任务ID
     * @return 完成后的文件信息
     */
    UploadFile completeUpload(String taskId, HttpServletRequest request);

    /**
     * Description:
     * 取消上传任务
     * @author SkyDev
     * @date 2025-09-29 16:27:21
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void cancelUpload(String taskId, Long userId);

    /**
     * Description:
     * 清理过期的上传任务
     * @author SkyDev
     * @date 2025-09-29 16:27:26
     */
    void cleanExpiredTasks();

    /**
     * Description:
     * 获取用户的上传任务列表
     * @author SkyDev
     * @date 2025-09-29 16:27:33
     * @param userId 用户ID
     * @return 任务列表
     */
    List<UploadTaskDTO> getUserUploadTasks(Long userId);

    /**
     * Description:
     * 恢复上传任务
     * @author SkyDev
     * @date 2025-09-29 16:27:43
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 上传准备响应
     */
    UploadPrepareResponse resumeTask(String taskId, Long userId);

    /**
     * Description:
     * 批量删除任务
     * @author SkyDev
     * @date 2025-09-29 16:27:51
     * @param taskIds 任务ID列表
     * @param userId 用户ID
     */
    void deleteTasks(List<String> taskIds, Long userId);
}