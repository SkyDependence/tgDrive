package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.ChunkUploadRequest;
import com.skydevs.tgdrive.entity.UploadSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface ChunkStorageService {
    /**
     * 保存分片文件
     * @param request 分片上传请求
     */
    void saveChunk(ChunkUploadRequest request);

    /**
     * 检查已上传的分片
     * @param identifier 文件唯一标识
     * @return 已上传的分片编号集合
     */
    Set<Integer> checkUploadedChunks(String identifier);

    /**
     * 合并分片
     * @param identifier 文件唯一标识
     * @param filename 文件名
     * @param totalChunks 总分片数
     * @return 合并后的文件输入流
     * @throws IOException
     */
    InputStream mergeChunks(String identifier, String filename, int totalChunks) throws IOException;

    /**
     * 清理分片文件
     * @param identifier 文件唯一标识
     */
    void cleanupChunks(String identifier);

    /**
     * Description:
     * 创建断点续传上传文件会话
     * @author SkyDev
     * @date 2025-08-26 14:28:21
     * @param filename 文件名
     * @param totalSize 文件大小
     * @param userId 用户id
     * @return 断点续传上传文件会话
     */
    UploadSession createUploadSession(String filename, Long totalSize, Long userId);

    /**
     * Description:
     * 获取所有分块的文件ID
     * @author SkyDev
     * @date 2025-08-26 15:00:00
     * @param identifier 会话标识
     * @return 分块文件ID列表
     */
    List<String> getChunkFileIds(String identifier);

    int cleanupExpiredSessions();

    boolean isChunkExists(String identifier, Integer chunkNumber);

    String saveChunkToTelegram(String identifier, Integer chunkNumber, InputStream chunkData, String chunkFilename);

    UploadSession getUploadSession(String identifier);
}
