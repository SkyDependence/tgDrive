package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.result.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * 负责文件的存储、检索和管理，与具体的存储实现解耦
 */
public interface FileStorageService {

    /**
     * Description:
     * 获取上传文件（文件名，上传链接）
     * @author SkyDev
     * @date 2025-07-30 15:08:45
     */
    UploadFile getUploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId);

    /**
     * 上传文件
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param size 文件大小
     * @return 文件存储ID
     */
    String uploadFile(InputStream inputStream, String filename, long size);

    /**
     * 分页查询文件列表
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    PageResult getFileList(int page, int size, String keyword, Long userId, String role);

    /**
     * 更新文件url
     */
    void updateUrl(HttpServletRequest request);

    /**
     * 根据文件ID删除文件
     * @param fileId 文件ID
     */
    void deleteFile(String fileId, Long userId, String role);

    /**
     * Description:
     * 文件假删除
     * @author SkyDev
     * @date 2025-07-30 16:29:07
     * @param fileId 文件id
     * @param isPublic 是否公开
     * @param userId 用户id
     * @param role 权限
     */
    void updateIsPublic(String fileId, boolean isPublic, Long userId, String role);
}