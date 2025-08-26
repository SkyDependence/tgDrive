package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.ChunkUploadRequest;
import com.skydevs.tgdrive.entity.FileChunk;
import com.skydevs.tgdrive.entity.UploadSession;
import com.skydevs.tgdrive.mapper.UploadSessionMapper;
import com.skydevs.tgdrive.service.ChunkStorageService;
import com.skydevs.tgdrive.service.DownloadService;
import com.skydevs.tgdrive.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkStorageServiceImpl implements ChunkStorageService {
    private final UploadSessionMapper uploadSessionMapper;
    private final FileStorageService fileStorageService;
    private final DownloadService downloadService;

    private final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    @Transactional
    public void saveChunk(ChunkUploadRequest request) {
        // This method is deprecated - use saveChunkToTelegram instead
        throw new UnsupportedOperationException("saveChunk is deprecated. Use saveChunkToTelegram instead.");
    }

    @Override
    public Set<Integer> checkUploadedChunks(String identifier) {
        UploadSession session = uploadSessionMapper.getSessionByIdentifier(identifier);
        if (session == null) {
            return Collections.emptySet();
        }
        
        List<Integer> chunks = uploadSessionMapper.getCompletedChunkNumbers(session.getId());
        return new HashSet<>(chunks);
    }

    @Override
    @Transactional
    public InputStream mergeChunks(String identifier, String filename, int totalChunks) throws IOException {
        // This method is no longer needed for the new implementation
        // We create BigFileInfo directly instead of merging chunks
        throw new UnsupportedOperationException("mergeChunks is not supported in the new implementation. Use getChunkFileIds instead.");
    }

    @Override
    @Transactional
    public void cleanupChunks(String identifier) {
        UploadSession session = uploadSessionMapper.getSessionByIdentifier(identifier);
        if (session != null) {
            uploadSessionMapper.deleteChunksBySessionId(session.getId());
            uploadSessionMapper.deleteSession(identifier);
        }
    }

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
    public UploadSession createUploadSession(String filename, Long totalSize, Long userId) {
        String identifier = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) totalSize / CHUNK_SIZE);
        Long now = System.currentTimeMillis() / 1000;

        UploadSession session = UploadSession.builder()
                .identifier(identifier)
                .filename(filename)
                .totalSize(totalSize)
                .totalChunks(totalChunks)
                .chunkSize(CHUNK_SIZE)
                .userId(userId)
                .status("uploading")
                .createdAt(now)
                .updatedAt(now)
                .build();

        uploadSessionMapper.insertSession(session);
        return session;
    }

    /**
     * Save chunk to Telegram
     */
    @Transactional
    public String saveChunkToTelegram(String identifier, Integer chunkNumber, InputStream chunkData, String chunkFilename) {
        UploadSession session = uploadSessionMapper.getSessionByIdentifier(identifier);
        if (session == null) {
            throw new RuntimeException("Upload session not found: " + identifier);
        }

        // Upload chunk to Telegram
        String chunkId = fileStorageService.uploadFile(chunkData, chunkFilename, session.getChunkSize());

        // Save chunk record
        FileChunk chunk = FileChunk.builder()
                .sessionId(session.getId())
                .chunkNumber(chunkNumber)
                .chunkId(chunkId)
                .chunkSize(session.getChunkSize())
                .status("completed")
                .createdAt(System.currentTimeMillis() / 1000)
                .build();

        uploadSessionMapper.insertChunk(chunk);

        // Update session time
        uploadSessionMapper.updateSessionStatus(identifier, "uploading", System.currentTimeMillis() / 1000);

        return chunkId;
    }

    /**
     * Check if chunk exists
     */
    public boolean isChunkExists(String identifier, Integer chunkNumber) {
        return uploadSessionMapper.checkChunkExists(identifier, chunkNumber) > 0;
    }

    /**
     * Get upload session info
     */
    public UploadSession getUploadSession(String identifier) {
        return uploadSessionMapper.getSessionByIdentifier(identifier);
    }

    /**
     * Get all chunk file IDs for a session
     */
    public List<String> getChunkFileIds(String identifier) {
        UploadSession session = uploadSessionMapper.getSessionByIdentifier(identifier);
        if (session == null) {
            throw new RuntimeException("Upload session not found: " + identifier);
        }

        List<FileChunk> chunks = uploadSessionMapper.getChunksBySessionId(session.getId());
        // Sort by chunk number to ensure correct order
        chunks.sort(Comparator.comparingInt(FileChunk::getChunkNumber));
        
        return chunks.stream()
                .map(FileChunk::getChunkId)
                .collect(Collectors.toList());
    }

    /**
     * Cleanup expired sessions
     * @return number of cleaned sessions
     */
    @Transactional
    public int cleanupExpiredSessions() {
        // Clean up uploads unfinished after 7 days
        Long expireTime = System.currentTimeMillis() / 1000 - (7 * 24 * 60 * 60);
        return uploadSessionMapper.cleanupExpiredSessions(expireTime);
    }
}
