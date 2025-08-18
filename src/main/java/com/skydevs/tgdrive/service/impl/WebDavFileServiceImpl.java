package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.file.FailedToGetSizeException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.DownloadService;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.service.WebDavFileService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebDavFileServiceImpl implements WebDavFileService {
    private final FileMapper fileMapper;
    private final FileStorageService fileStorageService;
    private final TelegramBotService telegramBotService;
    private final DownloadService downloadService;

    @Override
    public String uploadByWebDav(InputStream inputStream, HttpServletRequest request) {
        try {
            String path = StringUtil.getPath(request.getRequestURI());

            long size = request.getContentLengthLong();
            if (size < 0) {
                log.error("无法获取文件大小");
                throw new FailedToGetSizeException();
            }
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            String fileId = fileStorageService.uploadFile(inputStream, fileName, size);
            List<FileInfo> fileInfos = fileMapper.getFilesByPathPrefix(path);
            for (FileInfo fileInfo : fileInfos) {
                fileMapper.deleteFile(fileInfo.getFileId());
            }
            // 提取文件夹名字（如果有文件夹的话）
            List<String> dirPaths = StringUtil.getDirsPathFromPath(path);
            for (String dirPath : dirPaths) {
                FileInfo dirInfo = fileMapper.getFileByWebdavPath(dirPath);
                if (dirInfo != null) {
                    continue;
                }
                dirInfo = FileInfo.builder().fileId("dir")
                        .fileName(StringUtil.getDisplayName(dirPath, true))
                        .downloadUrl("dir")
                        .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                        .size("0")
                        .fullSize(0L)
                        .webdavPath(dirPath)
                        .dir(true)
                        .userId(null) // WebDAV目录不关联用户
                        .isPublic(true) // WebDAV目录默认公开
                        .build();
                fileMapper.insertFile(dirInfo);
                log.info("新增文件夹路径{}", dirPath);
            }

            // 优先使用自定义URL，如果没有配置则使用请求中的URL
            String customUrl = telegramBotService.getCustomUrl();
            String prefix = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : StringUtil.getPrefix(request);
            
            // WebDAV上传的文件默认设置为公开，因为WebDAV通常用于共享
            FileInfo fileInfo = FileInfo.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .fullSize(size)
                    .size(UserFriendly.humanReadableFileSize(size))
                    .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                    .downloadUrl(prefix + "/d/" + fileId)
                    .webdavPath(path)
                    .userId(null) // WebDAV上传暂时不关联用户
                    .isPublic(true) // WebDAV文件默认公开
                    .build();
            fileMapper.insertFile(fileInfo);
            return fileId;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * WebDAV下载
     * @param path 文件路径
     * @return
     */
    @Override
    public ResponseEntity<StreamingResponseBody> downloadByWebDav(String path) {
        try {
            FileInfo fileInfo = fileMapper.getFileByWebdavPath(path);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            return downloadService.downloadFile(fileInfo.getFileId());
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.status(500).build();
        }
    }

    @Override
    public void deleteByWebDav(String path) {
        try {
            fileMapper.deleteFileByWebDav(path);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 列出WebDAV文件
     *
     * @param path 路径
     * @return
     */
    @Override
    public List<FileInfo> listFiles(String path) {
        List<FileInfo> files = fileMapper.getFilesByPathPrefix(path);
        if (files == null) {
            log.error("文件查询失败");
            return null;
        }
        List<FileInfo> res = new ArrayList<>();
        for (FileInfo file : files) {
            String str = file.getWebdavPath().substring(path.length());
            if (str.indexOf('/') != -1 && !file.isDir()) {
                continue;
            }
            if (str.indexOf('/') != -1 && str.substring(str.indexOf('/')).length() > 1) {
                continue;
            }
            if (file.getWebdavPath().equals(path)) {
                continue;
            }
            res.add(file);
        }
        return res;
    }
}
