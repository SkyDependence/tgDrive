package com.skydevs.tgdrive.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.FailedToGetSizeException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.service.BotService;
import com.skydevs.tgdrive.service.DownloadService;
import com.skydevs.tgdrive.service.FileService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FileServiceImpl implements FileService {
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private BotService botService;
    @Autowired
    private DownloadService downloadService;

    /**
     * 获取文件分页
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageResult getFileList(int page, int size) {
        // 设置分页
        PageHelper.startPage(page, size);
        Page<FileInfo> pageInfo = fileMapper.getAllFiles();
        List<FileInfo> fileInfos = new ArrayList<>();
        for (FileInfo fileInfo : pageInfo) {
            FileInfo fileInfo1 = new FileInfo();
            BeanUtils.copyProperties(fileInfo, fileInfo1);
            fileInfos.add(fileInfo1);
        }
        log.info("文件分页查询");
        return new PageResult((int) pageInfo.getTotal(), fileInfos);
    }

    /**
     * 更新文件url
     * @return
     */
    @Override
    public void updateUrl(HttpServletRequest request) {
        String prefix = StringUtil.getPrefix(request);
        fileMapper.updateUrl(prefix);
    }

    @Override
    public String uploadByWebDav(InputStream inputStream, HttpServletRequest request) {
        try {
            String path = StringUtil.getPath(request.getRequestURI());

            long size = request.getContentLengthLong();
            if (size < 0) {
                log.error("无法获取文件大小");
                throw new FailedToGetSizeException();
            }
            String fileId = botService.uploadFile(inputStream, path, request);
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
                        .build();
                fileMapper.insertFile(dirInfo);
                log.info("新增文件夹路径{}", dirPath);
            }

            // 从路径中提取文件名
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            // 优先使用自定义URL，如果没有配置则使用请求中的URL
            String customUrl = botService.getCustomUrl();
            String prefix = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : StringUtil.getPrefix(request);
            FileInfo fileInfo = FileInfo.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .fullSize(size)
                    .size(UserFriendly.humanReadableFileSize(size))
                    .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                    .downloadUrl(prefix + "/d/" + fileId)
                    .webdavPath(path).build();
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

    /**
     * 根据文件ID删除文件
     * @param fileId 文件ID
     */
    @Override
    public void deleteFile(String fileId) {
        try {
            fileMapper.deleteFile(fileId);
            log.info("文件删除成功，fileId: {}", fileId);
        } catch (Exception e) {
            log.error("文件删除失败，fileId: {}", fileId, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }
}
