package com.skydevs.tgdrive.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pengrad.telegrambot.model.Message;
import com.skydevs.tgdrive.dto.ChunkUploadResponse;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.dto.UploadPrepareResponse;
import com.skydevs.tgdrive.dto.UploadTaskDTO;
import com.skydevs.tgdrive.entity.BigFileInfo;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.UploadTask;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.mapper.UploadTaskMapper;
import com.skydevs.tgdrive.service.ResumableUploadService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 断点续传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumableUploadServiceImpl implements ResumableUploadService {

    private final UploadTaskMapper uploadTaskMapper;
    private final FileMapper fileMapper;
    private final UploadProgressWebSocketHandler uploadProgressWebSocketHandler;
    private final FileStorageServiceImpl fileStorageService;

    // 分块大小：10MB
    private static final int CHUNK_SIZE = 10 * 1024 * 1024;
    // 任务过期时间：7天
    private static final long EXPIRE_DAYS = 7;
    // 使用ConcurrentHashMap来管理任务锁，避免全局同步
    private final Map<String, Object> taskLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public UploadPrepareResponse prepareUpload(String fileName, Long fileSize, String fileHash, Long userId, HttpServletRequest request) {
        UploadTask existingTask = uploadTaskMapper.selectByHashAndUserId(fileHash, userId);
        if (existingTask != null) {
            LocalDateTime expiresAt = existingTask.getExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                uploadTaskMapper.deleteById(existingTask.getId());
                taskLocks.remove(existingTask.getId());
            } else {
                if ("completed".equals(existingTask.getStatus()) && existingTask.getFinalFileId() != null) {
                    String downloadUrl = StringUtil.getPrefix(request) + existingTask.getFinalFileId();
                    List<Integer> completedChunks = parseUploadedChunks(existingTask.getUploadedChunks());
                    return UploadPrepareResponse.builder()
                            .taskId(existingTask.getId())
                            .resumable(false)
                            .completed(true)
                            .totalChunks(existingTask.getTotalChunks())
                            .uploadedChunks(completedChunks)
                            .chunkSize(existingTask.getChunkSize() != null ? existingTask.getChunkSize() : CHUNK_SIZE)
                            .finalFileId(existingTask.getFinalFileId())
                            .downloadUrl(downloadUrl)
                            .uploadedSize(existingTask.getFileSize())
                            .uploadProgress(100.0)
                            .build();
                }

                if ("failed".equals(existingTask.getStatus())) {
                    uploadTaskMapper.updateStatus(existingTask.getId(), "pending", null);
                    existingTask.setStatus("pending");
                    existingTask.setErrorMessage(null);
                }

                List<Integer> uploadedChunks = parseUploadedChunks(existingTask.getUploadedChunks());
                uploadedChunks.sort(Integer::compareTo);
                int chunkSize = existingTask.getChunkSize() != null ? existingTask.getChunkSize() : CHUNK_SIZE;
                long uploadedSize = Math.min(existingTask.getFileSize(), (long) uploadedChunks.size() * chunkSize);
                int totalChunks = existingTask.getTotalChunks() != null
                        ? existingTask.getTotalChunks()
                        : (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                double progress = totalChunks > 0 ? (double) uploadedChunks.size() / totalChunks * 100 : 0.0;

                return UploadPrepareResponse.builder()
                        .taskId(existingTask.getId())
                        .resumable(true)
                        .completed(false)
                        .totalChunks(totalChunks)
                        .uploadedChunks(uploadedChunks)
                        .chunkSize(chunkSize)
                        .uploadedSize(uploadedSize)
                        .uploadProgress(progress)
                        .build();
            }
        }

        String taskId = fileHash + "_" + userId + "_" + System.currentTimeMillis();

        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        UploadTask newTask = UploadTask.builder()
                .id(taskId)
                .userId(userId)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileHash(fileHash)
                .chunkSize(CHUNK_SIZE)
                .totalChunks(totalChunks)
                .uploadedChunks("[]")
                .chunkFileIds("{}")
                .status("pending")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(EXPIRE_DAYS))
                .build();

        uploadTaskMapper.insertUploadTask(newTask);

        return UploadPrepareResponse.builder()
                .taskId(taskId)
                .resumable(false)
                .completed(false)
                .totalChunks(totalChunks)
                .uploadedChunks(new ArrayList<>())
                .chunkSize(CHUNK_SIZE)
                .uploadedSize(0L)
                .uploadProgress(0.0)
                .build();
    }


    @Override
    @Transactional
    public ChunkUploadResponse uploadChunk(String taskId, Integer chunkIndex, MultipartFile chunk, String chunkHash) {
        // 获取或创建任务级别的锁
        Object taskLock = taskLocks.computeIfAbsent(taskId, k -> new Object());

        // 先检查任务状态和是否已上传（快速检查，不需要长时间锁定）
        UploadTask task;
        List<Integer> uploadedChunks;
        JSONObject chunkFileIds;

        synchronized (taskLock) {
            task = uploadTaskMapper.selectById(taskId);
            if (task == null) {
                throw new RuntimeException("上传任务不存在");
            }

            if ("completed".equals(task.getStatus())) {
                throw new RuntimeException("上传任务已完成");
            }

            // 解析已上传的分块
            uploadedChunks = parseUploadedChunks(task.getUploadedChunks());
            chunkFileIds = JSON.parseObject(task.getChunkFileIds() != null ? task.getChunkFileIds() : "{}");

            // 检查分块是否已上传
            if (uploadedChunks.contains(chunkIndex)) {
                return ChunkUploadResponse.builder()
                        .taskId(taskId)
                        .chunkIndex(chunkIndex)
                        .chunkFileId(chunkFileIds.getString(String.valueOf(chunkIndex)))
                        .success(true)
                        .message("分块已上传")
                        .uploadedChunksCount(uploadedChunks.size())
                        .progressPercentage((double) uploadedChunks.size() / task.getTotalChunks() * 100)
                        .build();
            }
        }

        // 上传分块到Telegram（这部分可以并行执行，不需要锁）
        try {
            String chunkName = task.getFileName() + "_part" + chunkIndex;
            byte[] chunkData = chunk.getBytes();

            Message message = fileStorageService.sendDocument(chunkData, chunkName);
            String fileId = StringUtil.extractFileId(message);

            if (fileId == null) {
                throw new RuntimeException("上传分块失败：无法获取文件ID");
            }

            // 只在更新数据库时加锁
            synchronized (taskLock) {
                // 重新读取最新的已上传分块信息（避免覆盖其他线程的更新）
                task = uploadTaskMapper.selectById(taskId);
                uploadedChunks = parseUploadedChunks(task.getUploadedChunks());
                chunkFileIds = JSON.parseObject(task.getChunkFileIds() != null ? task.getChunkFileIds() : "{}");

                // 再次检查是否已上传（防止重复）
                if (uploadedChunks.contains(chunkIndex)) {
                    return ChunkUploadResponse.builder()
                            .taskId(taskId)
                            .chunkIndex(chunkIndex)
                            .chunkFileId(chunkFileIds.getString(String.valueOf(chunkIndex)))
                            .success(true)
                            .message("分块已上传")
                            .uploadedChunksCount(uploadedChunks.size())
                            .progressPercentage((double) uploadedChunks.size() / task.getTotalChunks() * 100)
                            .build();
                }

                // 更新已上传分块信息
                uploadedChunks.add(chunkIndex);
                chunkFileIds.put(String.valueOf(chunkIndex), fileId);

                String uploadedChunksJson = JSON.toJSONString(uploadedChunks);
                String chunkFileIdsJson = chunkFileIds.toJSONString();

                uploadTaskMapper.updateUploadedChunks(taskId, uploadedChunksJson, chunkFileIdsJson);

                // 发送进度更新
                double progress = (double) uploadedChunks.size() / task.getTotalChunks() * 100;
                uploadProgressWebSocketHandler.sendUploadProgress(
                    task.getFileName(),
                    progress,
                    uploadedChunks.size(),
                    task.getTotalChunks()
                );

                log.info("分块上传成功 - 任务: {}, 分块: {}/{}", taskId, chunkIndex + 1, task.getTotalChunks());

                return ChunkUploadResponse.builder()
                        .taskId(taskId)
                        .chunkIndex(chunkIndex)
                        .chunkFileId(fileId)
                        .success(true)
                        .message("分块上传成功")
                        .uploadedChunksCount(uploadedChunks.size())
                        .progressPercentage(progress)
                        .build();
            }

        } catch (Exception e) {
            log.error("分块上传失败 - 任务: {}, 分块: {}", taskId, chunkIndex, e);
            synchronized (taskLock) {
                uploadTaskMapper.updateStatus(taskId, "failed", e.getMessage());
            }
            throw new RuntimeException("分块上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UploadFile completeUpload(String taskId, HttpServletRequest request) {
        // 获取任务级别的锁
        Object taskLock = taskLocks.get(taskId);
        if (taskLock == null) {
            taskLock = taskLocks.computeIfAbsent(taskId, k -> new Object());
        }

        synchronized (taskLock) {
            try {
                // 获取上传任务
                UploadTask task = uploadTaskMapper.selectById(taskId);
                if (task == null) {
                    throw new RuntimeException("上传任务不存在");
                }

                // 检查是否所有分块都已上传
                List<Integer> uploadedChunks = parseUploadedChunks(task.getUploadedChunks());
                if (uploadedChunks.size() != task.getTotalChunks()) {
                    throw new RuntimeException("还有分块未上传完成");
                }

                String finalFileId;

                if (task.getFileSize() <= CHUNK_SIZE) {
                    // 小文件，直接使用第一个分块的ID
                    JSONObject chunkFileIds = JSON.parseObject(task.getChunkFileIds());
                    finalFileId = chunkFileIds.getString("0");
                } else {
                    // 大文件，创建记录文件
                    finalFileId = createRecordFile(task);
                }

                // 更新任务状态
                uploadTaskMapper.completeTask(taskId, finalFileId);

                // 保存文件信息到files表
                String downloadUrl = StringUtil.getPrefix(request) + "/d/" + finalFileId;
                FileInfo fileInfo = FileInfo.builder()
                        .fileId(finalFileId)
                        .fileName(task.getFileName())
                        .size(UserFriendly.humanReadableFileSize(task.getFileSize()))
                        .fullSize(task.getFileSize())
                        .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                        .downloadUrl(downloadUrl)
                        .userId(task.getUserId())
                        .build();

                fileMapper.insertFile(fileInfo);

                // 发送完成消息
                uploadProgressWebSocketHandler.sendUploadComplete(task.getFileName());

                log.info("上传任务完成 - 任务: {}, 文件: {}", taskId, task.getFileName());

                // 完成后立即删除任务记录，避免占用空间
                uploadTaskMapper.deleteById(taskId);

                // 清理任务锁
                taskLocks.remove(taskId);

                return new UploadFile(task.getFileName(), downloadUrl);

            } catch (Exception e) {
                log.error("完成上传失败 - 任务: {}", taskId, e);
                uploadTaskMapper.updateStatus(taskId, "failed", e.getMessage());
                throw new RuntimeException("完成上传失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void cancelUpload(String taskId, Long userId) {
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        // 验证用户权限
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权限取消此上传任务");
        }

        uploadTaskMapper.deleteById(taskId);
        log.info("上传任务已取消 - 任务: {}, 用户: {}", taskId, userId);
    }

    @Override
    public void cleanExpiredTasks() {
        // 清理过期的未完成任务
        int deleted = uploadTaskMapper.deleteExpiredTasks(LocalDateTime.now());
        if (deleted > 0) {
            log.info("清理过期上传任务: {} 个", deleted);
        }
    }

    @Override
    public List<UploadTaskDTO> getUserUploadTasks(Long userId) {
        List<UploadTask> tasks = uploadTaskMapper.selectByUserId(userId);
        return tasks.stream().map(this::convertToDTO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public UploadPrepareResponse resumeTask(String taskId, Long userId) {
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("上传任务不存在");
        }

        // 验证用户权限
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权限访问此任务");
        }

        if ("completed".equals(task.getStatus())) {
            // 已完成的任务不能恢复，需要重新上传
            throw new RuntimeException("任务已完成，请重新上传");
        }

        if ("failed".equals(task.getStatus())) {
            // 重置失败状态为pending
            uploadTaskMapper.updateStatus(taskId, "pending", null);
        }

        List<Integer> uploadedChunks = parseUploadedChunks(task.getUploadedChunks());
        long uploadedSize = (long) uploadedChunks.size() * CHUNK_SIZE;
        double progress = (double) uploadedChunks.size() / task.getTotalChunks() * 100;

        return UploadPrepareResponse.builder()
                .taskId(taskId)
                .resumable(true)
                .completed(false)
                .totalChunks(task.getTotalChunks())
                .uploadedChunks(uploadedChunks)
                .chunkSize(CHUNK_SIZE)
                .uploadedSize(uploadedSize)
                .uploadProgress(progress)
                .build();
    }

    @Override
    public void deleteTasks(List<String> taskIds, Long userId) {
        for (String taskId : taskIds) {
            UploadTask task = uploadTaskMapper.selectById(taskId);
            if (task != null && task.getUserId().equals(userId)) {
                uploadTaskMapper.deleteById(taskId);
                log.info("删除上传任务: {}", taskId);
            }
        }
    }

    /**
     * 将UploadTask转换为DTO
     */
    private UploadTaskDTO convertToDTO(UploadTask task) {
        List<Integer> uploadedChunks = parseUploadedChunks(task.getUploadedChunks());
        int uploadedCount = uploadedChunks.size();
        double progress = task.getTotalChunks() > 0 ? (double) uploadedCount / task.getTotalChunks() * 100 : 0;

        long uploadedSize = (long) uploadedCount * CHUNK_SIZE;
        long remainingSize = task.getFileSize() - uploadedSize;

        String statusText = switch (task.getStatus()) {
            case "pending" -> "等待上传";
            case "uploading" -> "上传中";
            case "paused" -> "已暂停";
            case "completed" -> "已完成";
            case "failed" -> "上传失败";
            default -> "未知状态";
        };

        boolean resumable = !"completed".equals(task.getStatus()) && task.getExpiresAt().isAfter(LocalDateTime.now());

        return UploadTaskDTO.builder()
                .id(task.getId())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .fileSizeStr(UserFriendly.humanReadableFileSize(task.getFileSize()))
                .totalChunks(task.getTotalChunks())
                .uploadedChunks(uploadedCount)
                .progress(progress)
                .status(task.getStatus())
                .statusText(statusText)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .expiresAt(task.getExpiresAt())
                .errorMessage(task.getErrorMessage())
                .resumable(resumable)
                .remainingSize(remainingSize)
                .remainingSizeStr(UserFriendly.humanReadableFileSize(remainingSize))
                .build();
    }

    /**
     * 创建记录文件（用于大文件）
     */
    private String createRecordFile(UploadTask task) throws IOException {
        JSONObject chunkFileIds = JSON.parseObject(task.getChunkFileIds());
        List<String> fileIds = new ArrayList<>();

        // 按顺序提取所有分块的fileId
        for (int i = 0; i < task.getTotalChunks(); i++) {
            String fileId = chunkFileIds.getString(String.valueOf(i));
            if (fileId == null) {
                throw new RuntimeException("分块 " + i + " 的fileId缺失");
            }
            fileIds.add(fileId);
        }

        // 创建记录文件内容
        BigFileInfo record = new BigFileInfo();
        record.setFileName(task.getFileName());
        record.setFileSize(task.getFileSize());
        record.setFileIds(fileIds);
        record.setRecordFile(true);

        // 创建临时文件
        Path tempDir = Files.createTempDirectory("upload_record");
        String hashString = DigestUtil.sha256Hex(task.getFileName());
        Path tempFile = tempDir.resolve(hashString + ".record.json");

        String jsonString = JSON.toJSONString(record, true);
        Files.write(tempFile, jsonString.getBytes());

        // 上传记录文件到Telegram
        byte[] fileBytes = Files.readAllBytes(tempFile);
        Message message = fileStorageService.sendDocument(fileBytes, tempFile.getFileName().toString());
        String recordFileId = StringUtil.extractFileId(message);

        // 删除临时文件
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);

        log.info("记录文件创建成功 - FileID: {}", recordFileId);

        return recordFileId;
    }

    /**
     * 解析已上传的分块索引
     */
    private List<Integer> parseUploadedChunks(String uploadedChunksJson) {
        if (uploadedChunksJson == null || uploadedChunksJson.isEmpty() || "[]".equals(uploadedChunksJson)) {
            return new ArrayList<>();
        }
        return JSONArray.parseArray(uploadedChunksJson, Integer.class);
    }
}