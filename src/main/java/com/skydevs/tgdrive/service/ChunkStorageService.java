package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.ChunkUploadRequest;

import java.io.InputStream;
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
     */
    InputStream mergeChunks(String identifier, String filename, int totalChunks);

    /**
     * 清理分片文件
     * @param identifier 文件唯一标识
     */
    void cleanupChunks(String identifier);
}
