package com.skydevs.tgdrive.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.response.SendResponse;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.entity.BigFileInfo;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.BaseException;
import com.skydevs.tgdrive.exception.user.InsufficientPermissionException;
import com.skydevs.tgdrive.exception.file.UploadFileIsNullException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Telegram的文件存储服务实现
 */
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    @Autowired
    private FileMapper fileMapper;

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

    // 上传幂等去重：记录“正在上传中”的文件标识(userId:filename:size)。
    // 移动端网络切换/连接中断时，浏览器可能对同一个 multipart POST 自动重发，
    // 导致同一文件被完整上传两遍并写入两条记录。用内存锁拦截并发的重复上传，
    // 第二个请求直接失败，避免重复传 Telegram 与重复写库。
    private final java.util.concurrent.ConcurrentHashMap<String, Long> uploadingKeys =
            new java.util.concurrent.ConcurrentHashMap<>();

    private String buildUploadKey(Long userId, String filename, long size) {
        return (userId == null ? "anon" : userId) + ":" + filename + ":" + size;
    }

    @Override
    public UploadFile getUploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId) {
        UploadFile uploadFile = new UploadFile();
        String downloadUrl;
        if (multipartFile != null && !multipartFile.isEmpty()) {
            String filename = multipartFile.getOriginalFilename();
            long size = multipartFile.getSize();
            // 幂等锁：同一 (userId, filename, size) 正在上传时，拒绝并发重复上传，
            // 防止移动端连接中断导致的 multipart 自动重发把同一文件传两遍
            String uploadKey = buildUploadKey(userId, filename, size);
            if (uploadingKeys.putIfAbsent(uploadKey, System.currentTimeMillis()) != null) {
                log.warn("检测到重复上传请求，已忽略。userId={}, filename={}, size={}", userId, filename, size);
                throw new BaseException("该文件正在上传中，请勿重复提交");
            }
            try (InputStream inputStream = multipartFile.getInputStream()) {
                // 优先使用自定义URL，如果没有配置则使用请求中的URL
                String prefix = StringUtil.getPrefix(request);

                // 使用FileStorageService上传文件（传入 userId 用于按用户推送上传进度）
                String fileID;
                if (size > MAX_FILE_SIZE) {
                    fileID = uploadLargeFile(inputStream, filename, size, userId);
                } else {
                    fileID = uploadSmallFile(inputStream, filename, userId);
                }
                
                // 无论大小，上传流程成功后发送完成消息
                uploadProgressWebSocketHandler.sendUploadComplete(userId, filename);

                downloadUrl = prefix + "/d/" + fileID;

                // 保存文件信息到数据库
                FileInfo fileInfo = FileInfo.builder()
                        .fileId(fileID)
                        .size(UserFriendly.humanReadableFileSize(size))
                        .fullSize(size)
                        .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                        .downloadUrl(downloadUrl)
                        .fileName(filename)
                        .userId(userId)
                        .build();
                fileMapper.insertFile(fileInfo);
            } catch (IOException e) {
                log.error("文件上传失败，响应信息：{}", e.getMessage());
                throw new RuntimeException("文件上传失败");
            } finally {
                // 上传结束（成功或失败）后释放幂等锁
                uploadingKeys.remove(uploadKey);
            }
        } else {
            throw new UploadFileIsNullException();
        }

        uploadFile.setFileName(multipartFile.getOriginalFilename());
        uploadFile.setDownloadLink(downloadUrl);
        return uploadFile;
    }

    public String uploadFile(InputStream inputStream, String filename, long size) {
        // 接口方法无 userId 上下文，传 null 表示不按用户推送进度
        if (size > MAX_FILE_SIZE) {
            return uploadLargeFile(inputStream, filename, size, null);
        } else {
            return uploadSmallFile(inputStream, filename, null);
        }
    }

    private String uploadLargeFile(InputStream inputStream, String filename, long size, Long userId) {
        try {
            List<String> fileIds = sendFileStreamInChunks(inputStream, filename, size, userId);
            return createRecordFile(filename, size, fileIds);
        } catch (Exception e) {
            log.error("大文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("大文件上传失败", e);
        }
    }

    /**
     * 上传小文件
     */
    private String uploadSmallFile(InputStream inputStream, String filename, Long userId) {
        try {
            // 发送单文件上传进度
            uploadProgressWebSocketHandler.sendUploadProgress(userId, filename, 0, 0, 1);

            // 小于10MB的GIF会被TG转换为MP4，对文件后缀进行处理
            String uploadFilename = filename;
            if (filename != null && filename.endsWith(".gif")) {
                uploadFilename = filename.substring(0, filename.lastIndexOf(".gif"));
            }

            Message message = sendDocument(inputStream, uploadFilename);
            String fileID = StringUtil.extractFileId(message);
            Integer messageID=message.messageId();

            // 发送上传完成进度
            uploadProgressWebSocketHandler.sendUploadProgress(userId, filename, 100, 1, 1);
            uploadProgressWebSocketHandler.sendUploadComplete(userId, filename);

            log.info("小文件上传成功，File ID：{}， 文件名：{}", fileID, filename);
            return fileID;
        } catch (Exception e) {
            log.error("小文件上传失败: {}", e.getMessage(), e);
            uploadProgressWebSocketHandler.sendUploadError(userId, filename, "文件上传失败: " + e.getMessage());
            throw new RuntimeException("小文件上传失败", e);
        }
    }

    /**
     * 分块上传文件（流式：边读边传，不再一次性把整个文件的所有分块缓存进内存）。
     * 内存占用上限约为 PERMITS × MAX_FILE_SIZE（并发在传的分块），与文件总大小解耦，避免大文件 OOM。
     * 通过信号量限制“已读入内存但尚未上传完成”的分块数量，形成对读取速度的背压。
     */
    private List<String> sendFileStreamInChunks(InputStream inputStream, String filename, long size, Long userId) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        // 信号量控制在途分块数量：读取线程在提交新分块前必须先拿到许可，
        // 从而保证同时驻留内存的分块不超过 PERMITS 个，实现读取-上传背压
        Semaphore semaphore = new Semaphore(PERMITS);

        final AtomicInteger completedChunks = new AtomicInteger(0);
        // 已提交（已读入内存并进入上传队列）的分块数
        final AtomicInteger submittedChunks = new AtomicInteger(0);
        // 预计算总分块数：文件总大小已知，据此固定进度分母，避免前端“分片总数从小到大跳变”。
        // 空文件按 1 块处理，避免除零；实际读取块数理论上与此一致。
        final int expectedTotalChunks = size > 0
                ? (int) ((size + MAX_FILE_SIZE - 1) / MAX_FILE_SIZE)
                : 1;
        // 记录首个失败：任一分块上传失败即在此登记，读取循环据此 fail-fast，
        // 避免某分块已失败却仍把后续整个大文件读入内存并上传到 Telegram（浪费内存、流量与请求）
        final java.util.concurrent.atomic.AtomicReference<Throwable> firstError = new java.util.concurrent.atomic.AtomicReference<>();

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            int chunkIndex = 0;

            // 边读边传：读满一块（或读到流末尾）就立即提交上传，读完即释放该块内存
            while (true) {
                // fail-fast：已有分块失败则停止读取后续数据，尽早中断整个上传
                if (firstError.get() != null) {
                    break;
                }

                byte[] buffer = new byte[MAX_FILE_SIZE];
                int offset = 0;
                while (offset < MAX_FILE_SIZE) {
                    int byteRead = bufferedInputStream.read(buffer, offset, MAX_FILE_SIZE - offset);
                    if (byteRead == -1) {
                        break;
                    }
                    offset += byteRead;
                }

                if (offset == 0) {
                    break;
                }

                // 精确裁剪到实际读取长度（最后一块通常不足 MAX_FILE_SIZE）
                final byte[] chunkData = (offset == MAX_FILE_SIZE) ? buffer : Arrays.copyOf(buffer, offset);
                final String partName = filename + "_part" + chunkIndex;
                submittedChunks.incrementAndGet();

                // 背压：在途分块达到上限时阻塞读取，防止内存无界增长
                semaphore.acquire();

                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Message message = sendDocument(chunkData, partName);
                        String fileID = StringUtil.extractFileId(message);

                        if (fileID != null) {
                            log.info("分块上传成功，File ID：{}， 文件名：{}", fileID, partName);

                            int completed = completedChunks.incrementAndGet();
                            // 分母使用预计算的总块数，保证进度单调递增、总数稳定；
                            // 兜底取两者较大值，防止极端情况下 completed 超过预算值
                            int total = Math.max(expectedTotalChunks, submittedChunks.get());
                            double percentage = (double) completed / total * 100;
                            uploadProgressWebSocketHandler.sendUploadProgress(userId, filename, percentage, completed, total);

                            return fileID;
                        } else {
                            throw new RuntimeException("分块 " + partName + " 上传失败：无法获取文件ID");
                        }
                    } catch (Exception e) {
                        // 登记首个失败，触发读取循环 fail-fast
                        firstError.compareAndSet(null, e);
                        uploadProgressWebSocketHandler.sendUploadError(userId, filename, "分块 " + partName + " 上传失败");
                        throw new RuntimeException("分块 " + partName + " 上传失败", e);
                    } finally {
                        semaphore.release();
                    }
                }, uploadTaskExecutor);
                futures.add(future);
                chunkIndex++;
            }

            log.info("文件 {} 已按 {} 个分块提交上传", filename, futures.size());

            // 按提交顺序 join，保证 fileIds 与分块顺序一致（下载时据此顺序拼接）
            List<String> fileIds = new ArrayList<>(futures.size());
            try {
                for (CompletableFuture<String> future : futures) {
                    fileIds.add(future.join());
                }
                return fileIds;
            } catch (CompletionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                uploadProgressWebSocketHandler.sendUploadError(userId, filename, "分块上传失败: " + cause.getMessage());
                for (CompletableFuture<String> future : futures) {
                    future.cancel(true);
                }
                throw new RuntimeException("分块上传失败: " + cause.getMessage(), e);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("文件流读取失败或上传失败：{}", e.getMessage());
            uploadProgressWebSocketHandler.sendUploadError(userId, filename, "文件流读取失败或上传失败: " + e.getMessage());
            throw new RuntimeException("文件流读取失败或上传失败", e);
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

        // 创建一个系统临时目录
        Path tempDir = Files.createTempDirectory("tempDir");
        String hashString = DigestUtil.sha256Hex(originalFileName);
        Path tempFile = tempDir.resolve(hashString + ".record.json");
        Files.createFile(tempFile);

        try {
            String jsonString = JSON.toJSONString(record, true);
            Files.write(Paths.get(tempFile.toUri()), jsonString.getBytes());

            // 上传记录文件到 Telegram
            byte[] fileBytes = Files.readAllBytes(tempFile);
            Message message = sendDocument(fileBytes, tempFile.getFileName().toString());
            String recordFileId = StringUtil.extractFileId(message);

            log.info("记录文件上传成功，File ID: {}", recordFileId);
            return recordFileId;
        } catch (IOException e) {
            log.error("上传记录文件生成失败: {}", e.getMessage());
            throw new RuntimeException("上传文件生成失败", e);
        } finally {
            // 清理临时文件和目录，防止磁盘泄漏
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * 获取文件分页
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult getFileList(int page, int size, String keyword, Long userId, String role) {
//        todo 数据库丢了会很麻烦，1，文件无法展示，虽然现有的也图片展示也没什么用，但是无法获取图床链接还是很麻烦 2，不清楚webdav的同步机制是如何做的，核心问题问题在于fileinfo中是如何定义图片的链接，也就是从tg中获取文件，tg文件列表是否具备分级结构？
        /**
         * 1,假设数据库文件丢失
         * （1）真实文件： tg、webdav的本地挂载
         * （2）层级信息： 数据库、webdav的本地挂载
         *
         *
         *
         *   核心问题，备份文件的保存，无论如何这里只能解决webDAV的同步问题，如果层级信息本身无法保存，那么就无从恢复，
         *   想法1：
         *   场景： 数据库文件丢失，存在tg频道的真实文件，无webDAV
         *   使用第三方保存，例如github仓库，启动时恢复数据库
         *   场景2： 数据库文件丢失，tg频道未丢失，webDAV存在，
         *   webDAV同步至频道时的文件对应关系
         *   场景3： 数据库文件丢失，tg频道未丢失，webDAV未丢失，
         *   webDAV同步至频道时的文件对应关系
         *
         *

         *
         *   目前疑问：
         *   1，webDAV如何同步
         *   2，github设置定时任务同步仓库文件
         *
         *          *
         *          * 1，保证不同名，如果同名也不要紧，只要在tg频道中搜索同名文件即可
         *              问题： 挂载多次会导致上传多次，可能会触发tg频道的文件空间限制
         *          * 2，
         *          *
         *
         *   此外：
         *
         *  1，项目简介里的核心优势都是如何实现的？
         *
         *
         */

        PageHelper.startPage(page, size);
        List<FileInfo> fileInfoList = fileMapper.getFilteredFiles(keyword, userId, role);
        PageInfo<FileInfo> pageInfo = new PageInfo<>(fileInfoList);
        log.info("文件分页查询");
        return new PageResult((int) pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 更新文件url
     */
    @Override
    public void updateUrl(HttpServletRequest request) {
        String prefix = StringUtil.getPrefix(request);
        fileMapper.updateUrl(prefix);
    }

    /**
     * 根据文件ID删除文件
     * @param fileId 文件ID
     */
    @Override
    public void deleteFile(String fileId, Long userId, String role) {
        // 防御：WebDAV 目录记录的 fileId 全为 "dir"，按 fileId 删除会误删所有目录记录
        if ("dir".equals(fileId)) {
            throw new BaseException("不允许通过文件接口删除目录记录");
        }
        FileInfo file = fileMapper.getFileByFileId(fileId);
        if (file == null) {
            throw new BaseException("文件不存在");
        }
        if (file.isDir()) {
            throw new BaseException("不允许通过文件接口删除目录记录");
        }
        if ("admin".equals(role) || (file.getUserId() != null && file.getUserId().equals(userId))) {
            try {
                fileMapper.deleteFile(fileId);
                log.info("文件删除成功，fileId: {}", fileId);
            } catch (Exception e) {
                log.error("文件删除失败，fileId: {}", fileId, e);
                throw new BaseException("文件删除失败");
            }
        } else {
            throw new InsufficientPermissionException("无权限删除此文件");
        }
    }

    @Override
    public void updateIsPublic(String fileId, boolean isPublic, Long userId, String role) {
        FileInfo file = fileMapper.getFileByFileId(fileId);
        if (file == null) {
            throw new BaseException("文件不存在");
        }
        if ("admin".equals(role) || (file.getUserId() != null && file.getUserId().equals(userId))) {
            fileMapper.updateIsPublic(fileId, isPublic);
        } else {
            throw new InsufficientPermissionException("无权限更新此文件");
        }
    }

    /**
     * Description:
     * 调用bot上传文件
     * @author SkyDev
     * @date 2025-08-01 17:36:24
     * @param fileData 文件
     * @param filename 文件名
     * @return 上传文件的返回信息
     */
    private Message sendDocument(byte[] fileData, String filename) {
        TelegramBot bot = telegramBotService.getBot();
        String chatId = telegramBotService.getChatId();
        int retryCount = 3;
        int baseDelay = 1000;

        for (int i = 0; i < retryCount; i++) {
            try {
                SendDocument sendDocument = new SendDocument(chatId, fileData).fileName(filename);
                SendResponse response = bot.execute(sendDocument);

                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }

                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                log.warn("发送文档失败，正在准备第{}次重试，等待{}毫秒", (i+1), exponentialDelay);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                if (i == retryCount - 1) {
                    log.error("发送文档失败，已达到最大重试次数: {}", e.getMessage());
                    throw new RuntimeException("发送文档失败，已达到最大重试次数", e);
                }
                try {
                    Thread.sleep((long) baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        throw new RuntimeException("发送文档失败，已达到最大重试次数");
    }

    /**
     * Description:
     * 流上传
     * @author SkyDev
     * @date 2025-08-01 17:37:53
     * @param inputStream 文件流
     * @param filename 文件名
     * @return 上传文件的返回信息
     */
    private Message sendDocument(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendDocument(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }
}
