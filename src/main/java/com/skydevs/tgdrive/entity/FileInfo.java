package com.skydevs.tgdrive.entity;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileInfo {
    private String fileName;         // 文件名
    private String downloadUrl;      // 文件下载URL
    private String size;
    private Long fullSize;
    private String fileId;
    private Integer messageId;

    // 用于存储上传时间的 UNIX 时间戳
    private Long uploadTime;

    // WebDAV文件路径
    private String webdavPath;

    private boolean dir;

    private Long userId;

    private boolean isPublic = false;
    
    private String uploader; // 上传者用户名
}

