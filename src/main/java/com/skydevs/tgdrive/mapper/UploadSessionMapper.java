package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.FileChunk;
import com.skydevs.tgdrive.entity.UploadSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 上传会话 Mapper
 */
@Mapper
public interface UploadSessionMapper {

    /**
     * 插入上传会话
     */
    @Insert("INSERT INTO upload_sessions (identifier, filename, total_size, total_chunks, chunk_size, user_id, status, created_at, updated_at) " +
            "VALUES (#{identifier}, #{filename}, #{totalSize}, #{totalChunks}, #{chunkSize}, #{userId}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSession(UploadSession session);

    /**
     * 根据标识符获取会话
     */
    @Select("SELECT * FROM upload_sessions WHERE identifier = #{identifier}")
    UploadSession getSessionByIdentifier(@Param("identifier") String identifier);

    /**
     * 更新会话状态
     */
    @Update("UPDATE upload_sessions SET status = #{status}, updated_at = #{updatedAt} WHERE identifier = #{identifier}")
    void updateSessionStatus(@Param("identifier") String identifier, 
                           @Param("status") String status, 
                           @Param("updatedAt") Long updatedAt);

    /**
     * 更新会话的 Telegram 文件 ID
     */
    @Update("UPDATE upload_sessions SET telegram_file_id = #{telegramFileId}, file_identifier = #{fileIdentifier}, " +
            "status = #{status}, updated_at = #{updatedAt} WHERE identifier = #{identifier}")
    void updateSessionComplete(@Param("identifier") String identifier,
                             @Param("telegramFileId") String telegramFileId,
                             @Param("fileIdentifier") String fileIdentifier,
                             @Param("status") String status,
                             @Param("updatedAt") Long updatedAt);

    /**
     * 删除会话
     */
    @Delete("DELETE FROM upload_sessions WHERE identifier = #{identifier}")
    void deleteSession(@Param("identifier") String identifier);

    /**
     * 获取用户的会话列表
     */
    @Select("SELECT * FROM upload_sessions WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UploadSession> getSessionsByUserId(@Param("userId") Long userId);

    /**
     * 清理过期会话
     * @return 清理的会话数量
     */
    @Delete("DELETE FROM upload_sessions WHERE status = 'uploading' AND updated_at < #{expireTime}")
    int cleanupExpiredSessions(@Param("expireTime") Long expireTime);

    /**
     * 插入分块记录
     */
    @Insert("INSERT INTO file_chunks (session_id, chunk_number, chunk_id, chunk_size, status, created_at) " +
            "VALUES (#{sessionId}, #{chunkNumber}, #{chunkId}, #{chunkSize}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertChunk(FileChunk chunk);

    /**
     * 获取会话的所有分块
     */
    @Select("SELECT * FROM file_chunks WHERE session_id = #{sessionId} ORDER BY chunk_number")
    List<FileChunk> getChunksBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 获取已完成的分块编号列表
     */
    @Select("SELECT chunk_number FROM file_chunks WHERE session_id = #{sessionId} AND status = 'completed' ORDER BY chunk_number")
    List<Integer> getCompletedChunkNumbers(@Param("sessionId") Long sessionId);

    /**
     * 检查分块是否存在
     */
    @Select("SELECT COUNT(*) FROM file_chunks fc " +
            "JOIN upload_sessions us ON fc.session_id = us.id " +
            "WHERE us.identifier = #{identifier} AND fc.chunk_number = #{chunkNumber} AND fc.status = 'completed'")
    int checkChunkExists(@Param("identifier") String identifier, @Param("chunkNumber") Integer chunkNumber);

    /**
     * 获取特定分块
     */
    @Select("SELECT fc.* FROM file_chunks fc " +
            "JOIN upload_sessions us ON fc.session_id = us.id " +
            "WHERE us.identifier = #{identifier} AND fc.chunk_number = #{chunkNumber}")
    FileChunk getChunkByNumber(@Param("identifier") String identifier, @Param("chunkNumber") Integer chunkNumber);

    /**
     * 删除会话的所有分块
     */
    @Delete("DELETE FROM file_chunks WHERE session_id = #{sessionId}")
    void deleteChunksBySessionId(@Param("sessionId") Long sessionId);
}