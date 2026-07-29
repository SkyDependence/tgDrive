package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.WebDavConfig;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.file.FailedToGetSizeException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.DownloadService;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.service.WebDavConfigService;
import com.skydevs.tgdrive.service.WebDavFileService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

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
    private final WebDavConfigService webDavConfigService;

    @Override
    public String uploadByWebDav(InputStream inputStream, HttpServletRequest request) {
        try {
            // URL 解码：getRequestURI() 返回未解码路径，中文等特殊字符以 %xx 编码
            String path = StringUtil.getPath(request.getRequestURI());
            try {
                path = UriUtils.decode(path, "UTF-8");
            } catch (Exception e) {
                log.warn("WebDAV 上传路径解码失败: {}", path);
            }

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
                telegramBotService.deleteFile(fileInfo.getMessageId());
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
                        .isPublic(false) // 不再标记为公开：WebDAV 记录不应混入 web 端个人文件列表
                        .build();
                fileMapper.insertFile(dirInfo);
                log.info("新增文件夹路径{}", dirPath);
            }

            // 优先使用自定义URL，如果没有配置则使用请求中的URL
            String customUrl = telegramBotService.getCustomUrl();
            String prefix = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : StringUtil.getPrefix(request);
            
            // WebDAV 上传的文件不再标记为公开：web 端“我的文件”只展示归属自己的文件，
            // WebDAV 文件仍可通过 WebDAV 协议或直链 /d/{fileId} 访问
            FileInfo fileInfo = FileInfo.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .fullSize(size)
                    .size(UserFriendly.humanReadableFileSize(size))
                    .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                    .downloadUrl(prefix + "/d/" + fileId)
                    .webdavPath(path)
                    .userId(null) // WebDAV上传暂时不关联用户
                    .isPublic(false) // 不再标记为公开
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
            FileInfo fileInfo = getFileByWebdavPathWithFallback(path);
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
        // 校验配置是否允许删除
        if (!isOperationAllowed(c -> c.getAllowDelete())) {
            throw new com.skydevs.tgdrive.exception.BaseException("WebDAV 删除操作已被禁用");
        }
        try {
            // 尝试删除文件，如果找不到则尝试解码后的路径
            FileInfo file = getFileByWebdavPathWithFallback(path);
            if (file != null) {
                fileMapper.deleteFileByWebDav(file.getWebdavPath());
            } else {
                // 如果还是找不到，尝试原始路径
                fileMapper.deleteFileByWebDav(path);
            }
        } catch (com.skydevs.tgdrive.exception.BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 列出 WebDAV 直接子项
     *
     * @param path 目录路径（建议以 / 结尾）
     * @return 直接子文件/子目录列表
     */
    @Override
    public List<FileInfo> listFiles(String path) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        // 根目录保持 "/"；非根目录统一以 / 结尾，保证 substring 计算正确
        if (!path.equals("/") && !path.endsWith("/")) {
            path = path + "/";
        }
        List<FileInfo> files = fileMapper.getFilesByPathPrefix(path);
        if (files == null) {
            log.error("文件查询失败");
            return new ArrayList<>();
        }
        List<FileInfo> res = new ArrayList<>();
        for (FileInfo file : files) {
            String str = file.getWebdavPath().substring(path.length());
            if (str.isEmpty()) {
                continue; // 自身
            }
            // 文件：余部不应包含 /，否则是更深层文件
            if (!file.isDir() && str.indexOf('/') != -1) {
                continue;
            }
            // 目录：余部形如 "sub/"；若包含更多内容（如 "sub/grand/"）则是更深层目录，跳过
            if (file.isDir()) {
                // str 例如 "sub/"，去掉末尾 / 后不应再包含 /
                String namePart = str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
                if (namePart.indexOf('/') != -1) {
                    continue;
                }
            }
            res.add(file);
        }
        return res;
    }

    /**
     * 检查 WebDAV 配置中的某项操作是否允许
     */
    private boolean isOperationAllowed(java.util.function.Function<WebDavConfig, Boolean> getter) {
        try {
            WebDavConfig config = webDavConfigService.getWebDavConfig();
            return config != null && Boolean.TRUE.equals(getter.apply(config));
        } catch (Exception e) {
            log.error("检查 WebDAV 操作权限失败", e);
            return false;
        }
    }

    /**
     * Description:
     * 尝试通过WebDAV路径查找文件，支持URL编码和大小写不敏感
     * @author SkyDev
     * @date 2025-09-01 10:00:49
     * @param path WebDAV路径
     * @return 文件信息，如果找不到则返回null
     */
    private FileInfo getFileByWebdavPathWithFallback(String path) {
        // 首先尝试原始路径
        FileInfo file = fileMapper.getFileByWebdavPath(path);
        if (file != null) {
            return file;
        }
        
        // 如果找不到，尝试URL解码后的路径
        try {
            String decodedPath = UriUtils.decode(path, "UTF-8");
            if (!decodedPath.equals(path)) {
                file = fileMapper.getFileByWebdavPath(decodedPath);
                if (file != null) {
                    log.info("Found file using decoded path: {} -> {}", path, decodedPath);
                    return file;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to decode URL: {}", path);
        }
        
        // 如果仍然找不到，尝试不区分大小写的查找
        try {
            // 获取父目录路径
            String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            
            // 获取父目录下的所有文件
            List<FileInfo> filesInDir = fileMapper.getFilesByPathPrefix(parentPath);
            for (FileInfo f : filesInDir) {
                if (f.getWebdavPath().equalsIgnoreCase(path)) {
                    log.info("Found file using case-insensitive match: {}", path);
                    return f;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to perform case-insensitive search for: {}", path);
        }
        
        return null;
    }
}
