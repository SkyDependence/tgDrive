package com.skydevs.tgdrive.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Message;
import com.skydevs.tgdrive.entity.BigFileInfo;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Telegram的文件存储服务实现
 */
@Service
@Slf4j
public class TelegramFileStorageServiceImpl implements FileStorageService {

    @Autowired
    private TelegramBotService telegramBotService;
    
    @Autowired
    private UploadProgressWebSocketHandler uploadProgressWebSocketHandler;
    
    @Autowired
    @Qualifier("uploadTaskExecutor")
    private ThreadPoolTaskExecutor uploadTaskExecutor;

    // tg bot接口限制20MB，传10MB是最佳实践
    private final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    // 控制同时运行的任务数量
    private final int PERMITS = 5;

    @Override
    public String uploadFile(InputStream inputStream, String filename, long size) {
        if (size > MAX_FILE_SIZE) {
            return uploadLargeFile(inputStream, filename, size);
        } else {
            return uploadSmallFile(inputStream, filename);
        }
    }

    @Override
    public String uploadLargeFile(InputStream inputStream, String filename, long size) {
        try {
            List<String> fileIds = sendFileStreamInChunks(inputStream, filename);
            return createRecordFile(filename, size, fileIds);
        } catch (Exception e) {
            log.error("大文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("大文件上传失败", e);
        }
    }

    @Override
    public InputStream downloadFile(String fileId) {
        try {
            File file = telegramBotService.getFile(fileId);
            String fileUrl = telegramBotService.getFullFilePath(file);
            return new URL(fileUrl).openStream();
        } catch (IOException e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    @Override
    public void deleteFile(String fileId) {
        try {
            telegramBotService.deleteMessage(fileId);
            log.info("文件删除成功，File ID: {}", fileId);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public String getFileInfo(String fileId) {
        try {
            File file = telegramBotService.getFile(fileId);
            return file.filePath();
        } catch (Exception e) {
            log.error("获取文件信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean fileExists(String fileId) {
        try {
            File file = telegramBotService.getFile(fileId);
            return file != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFileDownloadUrl(String fileId) {
        try {
            File file = telegramBotService.getFile(fileId);
            return telegramBotService.getFullFilePath(file);
        } catch (Exception e) {
            log.error("获取文件下载URL失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 上传小文件
     */
    private String uploadSmallFile(InputStream inputStream, String filename) {
        try {
            // 发送单文件上传进度
            uploadProgressWebSocketHandler.sendUploadProgress(filename, 0, 0, 1);
            
            // 小于10MB的GIF会被TG转换为MP4，对文件后缀进行处理
            String uploadFilename = filename;
            if (filename != null && filename.endsWith(".gif")) {
                uploadFilename = filename.substring(0, filename.lastIndexOf(".gif"));
            }
            
            Message message = telegramBotService.sendDocument(inputStream, uploadFilename);
            String fileID = telegramBotService.extractFileId(message);
            
            // 发送上传完成进度
            uploadProgressWebSocketHandler.sendUploadProgress(filename, 100, 1, 1);
            uploadProgressWebSocketHandler.sendUploadComplete(filename);
            
            log.info("小文件上传成功，File ID：{}， 文件名：{}", fileID, filename);
            return fileID;
        } catch (Exception e) {
            log.error("小文件上传失败: {}", e.getMessage(), e);
            uploadProgressWebSocketHandler.sendUploadError(filename, "文件上传失败: " + e.getMessage());
            throw new RuntimeException("小文件上传失败", e);
        }
    }

    /**
     * 分块上传文件
     */
    private List<String> sendFileStreamInChunks(InputStream inputStream, String filename) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(PERMITS);
        
        final AtomicInteger totalChunks = new AtomicInteger(0);
        final AtomicInteger completedChunks = new AtomicInteger(0);

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            byte[] buffer = new byte[MAX_FILE_SIZE];
            int partIndex = 0;
            List<byte[]> allChunks = new ArrayList<>();

            // 第一遍：读取所有分块数据
            while (true) {
                int offset = 0;
                while(offset < MAX_FILE_SIZE) {
                    int byteRead = bufferedInputStream.read(buffer, offset, MAX_FILE_SIZE - offset);
                    if (byteRead == -1) {
                        break;
                    }
                    offset += byteRead;
                }

                if (offset == 0) {
                    break;
                }
                
                byte[] chunkData = Arrays.copyOf(buffer, offset);
                allChunks.add(chunkData);
                partIndex++;
            }
            
            totalChunks.set(allChunks.size());
            log.info("文件 {} 将被分为 {} 个分块上传", filename, totalChunks.get());

            // 第二遍：提交所有上传任务
            for (int i = 0; i < allChunks.size(); i++) {
                final int chunkIndex = i;
                final byte[] chunkData = allChunks.get(i);
                final String partName = filename + "_part" + chunkIndex;
                
                semaphore.acquire();

                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Message message = telegramBotService.sendDocument(chunkData, partName);
                        String fileID = telegramBotService.extractFileId(message);
                        
                        if (fileID != null) {
                            log.info("分块上传成功，File ID：{}， 文件名：{}", fileID, partName);
                            
                            // 更新进度
                            int completed = completedChunks.incrementAndGet();
                            double percentage = (double) completed / totalChunks.get() * 100;
                            uploadProgressWebSocketHandler.sendUploadProgress(filename, percentage, completed, totalChunks.get());
                            
                            return fileID;
                        } else {
                            throw new RuntimeException("分块 " + partName + " 上传失败：无法获取文件ID");
                        }
                    } catch (Exception e) {
                        uploadProgressWebSocketHandler.sendUploadError(filename, "分块 " + partName + " 上传失败");
                        throw new RuntimeException("分块 " + partName + " 上传失败", e);
                    } finally {
                        semaphore.release();
                    }
                }, uploadTaskExecutor);
                futures.add(future);
            }

            // 等待所有任务完成并按顺序获取结果
            List<String> fileIds = new ArrayList<>();
            try {
                for (CompletableFuture<String> future : futures) {
                    fileIds.add(future.join());
                }
                return fileIds;
            } catch (CompletionException e) {
                uploadProgressWebSocketHandler.sendUploadError(filename, "分块上传失败: " + e.getCause().getMessage());
                for (CompletableFuture<String> future : futures) {
                    future.cancel(true);
                }
                throw new RuntimeException("分块上传失败: " + e.getCause().getMessage(), e);
            }
        } catch (IOException | InterruptedException e) {
            log.error("文件流读取失败或上传失败：{}", e.getMessage());
            uploadProgressWebSocketHandler.sendUploadError(filename, "文件流读取失败或上传失败: " + e.getMessage());
            throw new RuntimeException("文件流读取失败或上传失败", e);
        } finally {
            // 使用配置的线程池，不需要手动关闭
        }
    }

    /**
     * 创建记录文件
     */
    private String createRecordFile(String originalFileName, long fileSize, List<String> fileIds) throws IOException {
        BigFileInfo record = new BigFileInfo();
        record.setFileName(originalFileName);
        record.setFileSize(fileSize);
        record.setFileIds(fileIds);
        record.setRecordFile(true);

        // 创建一个系统临时文件
        Path tempDir = Files.createTempDirectory("tempDir");
        String hashString = DigestUtil.sha256Hex(originalFileName);
        Path tempFile = tempDir.resolve(hashString + ".record.json");
        Files.createFile(tempFile);
        
        try {
            String jsonString = JSON.toJSONString(record, true);
            Files.write(Paths.get(tempFile.toUri()), jsonString.getBytes());
        } catch (IOException e) {
            log.error("上传记录文件生成失败: {}", e.getMessage());
            throw new RuntimeException("上传文件生成失败", e);
        }

        // 上传记录文件到 Telegram
        byte[] fileBytes = Files.readAllBytes(tempFile);
        Message message = telegramBotService.sendDocument(fileBytes, tempFile.getFileName().toString());
        String recordFileId = telegramBotService.extractFileId(message);

        log.info("记录文件上传成功，File ID: {}", recordFileId);

        // 删除本地临时文件
        Files.deleteIfExists(tempFile);

        return recordFileId;
    }
}