package com.skydevs.tgdrive.service;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 * 负责文件的存储、检索和管理，与具体的存储实现解耦
 */
public interface FileStorageService {

    /**
     * 上传文件
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param size 文件大小
     * @return 文件存储ID
     */
    String uploadFile(InputStream inputStream, String filename, long size);

    /**
     * 上传大文件（分块上传）
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param size 文件大小
     * @return 文件存储ID
     */
    String uploadLargeFile(InputStream inputStream, String filename, long size);

    /**
     * 下载文件
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream downloadFile(String fileId);

    /**
     * 删除文件
     * @param fileId 文件ID
     */
    void deleteFile(String fileId);

    /**
     * 获取文件信息
     * @param fileId 文件ID
     * @return 文件路径或其他文件信息
     */
    String getFileInfo(String fileId);

    /**
     * 检查文件是否存在
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean fileExists(String fileId);

    /**
     * 获取文件下载URL
     * @param fileId 文件ID
     * @return 下载URL
     */
    String getFileDownloadUrl(String fileId);
}