package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.UploadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传任务Mapper
 */
@Mapper
public interface UploadTaskMapper {

    /**
     * 插入新的上传任务
     */
    int insertUploadTask(UploadTask uploadTask);

    /**
     * 根据ID查询上传任务
     */
    UploadTask selectById(@Param("id") String id);

    /**
     * 根据文件哈希和用户ID查询上传任务
     */
    UploadTask selectByHashAndUserId(@Param("fileHash") String fileHash, @Param("userId") Long userId);

    /**
     * 更新已上传分块信息
     */
    int updateUploadedChunks(@Param("id") String id,
                            @Param("uploadedChunks") String uploadedChunks,
                            @Param("chunkFileIds") String chunkFileIds);

    /**
     * 更新任务状态
     */
    int updateStatus(@Param("id") String id,
                    @Param("status") String status,
                    @Param("errorMessage") String errorMessage);

    /**
     * 完成上传任务
     */
    int completeTask(@Param("id") String id,
                    @Param("finalFileId") String finalFileId);

    /**
     * 删除过期的任务
     */
    int deleteExpiredTasks(@Param("now") LocalDateTime now);

    /**
     * 获取用户的所有上传任务
     */
    List<UploadTask> selectByUserId(@Param("userId") Long userId);

    /**
     * 删除上传任务
     */
    int deleteById(@Param("id") String id);

    /**
     * 删除旧的已完成任务
     */
    int deleteOldCompletedTasks(@Param("beforeDate") LocalDateTime beforeDate);
}