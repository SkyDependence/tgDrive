package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.WebDavFileService;
import com.skydevs.tgdrive.service.WebDavService;
import com.skydevs.tgdrive.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class WebDavServiceImpl implements WebDavService {

    private final WebDavFileService webDavFileService;
    private final FileMapper fileMapper;

    @Override
    public void switchMethod(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String realMethod = (String) request.getAttribute("X-HTTP-Method-Override");
        log.info("进入handleWebDav方法，真实的method是{}", realMethod);
        String realURI = request.getRequestURI().substring("/webdav/dispatch".length());
        log.info("请求路径是{}", realURI);

        if (realMethod == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-HTTP-Method-Override");
            return;
        }
        switch (realMethod.toUpperCase()) {
            case "PROPFIND":
                handlePropFind(request, response, realURI);
                break;
            case "MKCOL":
                handleMkCol(request, response, realURI);
                break;
            case "MOVE":
                handleMove(request, response, realURI);
                break;
            case "COPY":
                handleCopy(request, response, realURI);
                break;
            case "PROPPATCH":
                handlePropPatch(request, response, realURI);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unsupported WebDAV method");
                break;
        }
    }

    /**
     * 处理PROPPATCH请求，用于修改文件属性（如修改时间）
     * 我们的服务器实际上不支持修改，但为了兼容客户端，我们假装成功。
     * @param request
     * @param response
     * @param realURI
     * @throws IOException
     */
    private void handlePropPatch(HttpServletRequest request, HttpServletResponse response, String realURI) throws IOException {
        // 哼喵，我们其实什么都不用做，只要礼貌地回复一个成功就行了！
        response.setStatus(207); // 207 Multi-Status
        response.setContentType("application/xml;charset=UTF-8");

        // 构建一个最简单的“成功”XML回复
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:multistatus xmlns:D=\"DAV:\">" +
                "  <D:response>" +
                "    <D:href>" + escapeXml("/webdav" + realURI) + "</D:href>" +
                "    <D:propstat>" +
                "      <D:status>HTTP/1.1 200 OK</D:status>" +
                "    </D:propstat>" +
                "  </D:response>" +
                "</D:multistatus>";

        response.getWriter().write(xmlResponse);
    }

    /**
     * WebDAV文件移动
     * @param request
     * @param response
     * @param realURI
     */
    private void handleMove(HttpServletRequest request, HttpServletResponse response, String realURI) {
        String target = request.getHeader("Destination");
        String overwrite = request.getHeader("Overwrite");
        FileInfo sourceFile = fileMapper.getFileByWebdavPath(realURI);
        if (target == null || realURI == null || sourceFile == null) {
            response.setStatus(400);
            return;
        }
        target = getTargetPath(request, target, sourceFile.isDir());
        // 如果移动后和移动前路径相同，直接返回
        if (target.equals(realURI)) {
            response.setStatus(204);
            return;
        }
        FileInfo targetFile = fileMapper.getFileByWebdavPath(target);
        List<FileInfo> subFiles = getSubFiles(realURI);
        sourceFile.setFileName(StringUtil.getDisplayName(target, sourceFile.isDir()));
        if (targetFile != null && overwrite.equalsIgnoreCase("F")) {
            response.setStatus(409);
        } else if (overwrite.equalsIgnoreCase("T") && targetFile != null) {
            // 允许覆盖且目标路径有该文件名，删除原文件路径，更新目标文件路径的属性
            fileMapper.deleteFileByWebDav(realURI);
            fileMapper.updateFileAttributeByWebDav(sourceFile, target);
            handleMoveSubFiles(subFiles, target, realURI);
            response.setStatus(204);
            log.info("{} 移动到 {}", realURI, target);
        } else {
            // 目标路径没有该文件名
            fileMapper.deleteFileByWebDav(realURI);
            fileMapper.moveFile(sourceFile, target);
            handleMoveSubFiles(subFiles, target, realURI);
            response.setStatus(204);
            log.info("{} 移动到 {}", realURI, target);
        }
    }

    private List<FileInfo> getSubFiles(String realURI) {
        List<FileInfo> files =  fileMapper.getFilesByPathPrefix(realURI);
        files.removeIf(file -> file.getWebdavPath().equals(realURI));
        return files;
    }

    /**
     * 移动子文件
     * @param subFiles
     * @param target
     * @param realURI
     */
    private void handleMoveSubFiles(List<FileInfo> subFiles, String target, String realURI) {
        if (subFiles == null) {
            return;
        }
        log.info("开始移动子文件");
        for (FileInfo file : subFiles) {
            String targetPath = target;
            String sourcePath = file.getWebdavPath();
            targetPath = targetPath + sourcePath.substring(realURI.length());
            FileInfo targetFile = fileMapper.getFileByWebdavPath(targetPath);
            fileMapper.deleteFileByWebDav(sourcePath);
            if (targetFile != null) {
                fileMapper.updateFileAttributeByWebDav(file, targetPath);
            } else {
                fileMapper.moveFile(file, targetPath);
            }
        }
        log.info("子文件移动完成");
    }


    /**
     * 处理新建文件夹
     * @param request
     * @param response
     * @param realURI
     */
    private void handleMkCol(HttpServletRequest request, HttpServletResponse response, String realURI) {
        FileInfo fileInfo = fileMapper.getFileByWebdavPath(realURI);
        if (fileInfo != null) {
            response.setStatus(405);
            return;
        }
        fileInfo = FileInfo.builder().fileId("dir")
                .fileName(StringUtil.getDisplayName(realURI, true))
                .downloadUrl("dir")
                .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                .size("0")
                .fullSize(0L)
                .webdavPath(realURI)
                .dir(true)
                .build();
        fileMapper.insertFile(fileInfo);
        log.info("新增文件夹路径{}", realURI);
        response.setStatus(201);
    }

    /**
     * 处理文件复制
     * @param request
     * @param response
     */
    private void handleCopy(HttpServletRequest request, HttpServletResponse response, String realURI) {
        String target = request.getHeader("Destination");
        String overwrite = request.getHeader("Overwrite");
        FileInfo sourceFile = fileMapper.getFileByWebdavPath(realURI);
        if (target == null || realURI == null || sourceFile == null) {
            response.setStatus(400);
            return;
        }
        target = getTargetPath(request, target, sourceFile.isDir());
        // 如果移动后和移动前路径相同，直接返回
        if (target.equals(realURI)) {
            response.setStatus(204);
            return;
        }
        FileInfo targetFile = fileMapper.getFileByWebdavPath(target);
        List<FileInfo> subFiles = getSubFiles(realURI);
        sourceFile.setFileName(StringUtil.getDisplayName(target, sourceFile.isDir()));
        if (targetFile != null && overwrite.equalsIgnoreCase("F")) {
            response.setStatus(409);
        } else if (overwrite.equalsIgnoreCase("T") && targetFile != null) {
            // 允许覆盖且目标路径有该文件名，更新目标文件路径的属性
            fileMapper.updateFileAttributeByWebDav(sourceFile, target);
            handleCopySubFiles(subFiles, target, realURI);
            response.setStatus(204);
            log.info("{} 移动到 {}", realURI, target);
        } else {
            // 目标路径没有该文件名
            fileMapper.moveFile(sourceFile, target);
            handleCopySubFiles(subFiles, target, realURI);
            response.setStatus(204);
            log.info("{} 移动到 {}", realURI, target);
        }
    }

    private void handleCopySubFiles(List<FileInfo> subFiles, String target, String realURI) {
        if (subFiles == null) {
            return;
        }
        log.info("开始复制子文件");
        for (FileInfo file : subFiles) {
            String targetPath = target;
            String sourcePath = file.getWebdavPath();
            targetPath = targetPath + sourcePath.substring(realURI.length());
            FileInfo targetFile = fileMapper.getFileByWebdavPath(targetPath);
            if (targetFile != null) {
                fileMapper.updateFileAttributeByWebDav(file, targetPath);
            } else {
                fileMapper.moveFile(file, targetPath);
            }
        }
        log.info("子文件复制完成");
    }

    /**
     * 处理目录探测
     * @param request
     * @param response
     * @throws IOException
     */
    private static final DateTimeFormatter RFC1123_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    private void handlePropFind(HttpServletRequest request, HttpServletResponse response, String realURI) throws IOException {

        // 假设 path.equals("/") 总是存在的
        if (!realURI.equals("/") && !realURI.endsWith("/")) {
            // 这是一个对具体文件的PROPFIND请求
            FileInfo requestedFile = fileMapper.getFileByWebdavPath(realURI);
            if (requestedFile == null) {
                // 如果数据库里根本找不到这个文件，就必须返回 404！
                log.info("PROPFIND请求的文件不存在: {}", realURI);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        try {
            final String CONTEXT_PATH = "/webdav";

            response.setStatus(207); // 207 Multi-Status
            response.setContentType("application/xml;charset=UTF-8");

            String path = realURI;
            if (!StringUtils.hasText(path) || path.equals("/")) {
                path = "/";
            }

            List<FileInfo> files = webDavFileService.listFiles(path);

            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<D:multistatus xmlns:D=\"DAV:\">\n");

            String currentHref = path.equals("/") ? CONTEXT_PATH : CONTEXT_PATH + path;
            xmlBuilder.append("<D:response>\n")
                    .append("<D:href>").append(escapeXml(currentHref)).append("</D:href>\n")
                    .append("<D:propstat>\n")
                    .append("<D:prop>\n")
                    .append("<D:displayname>").append(escapeXml(path.equals("/") ? "" : getDisplayName(path))).append("</D:displayname>\n")
                    .append("<D:getlastmodified>").append(RFC1123_FORMATTER.format(Instant.now())).append("</D:getlastmodified>\n")
                    .append("<D:resourcetype><D:collection/></D:resourcetype>\n")
                    .append("</D:prop>\n")
                    .append("<D:status>HTTP/1.1 200 OK</D:status>\n")
                    .append("</D:propstat>\n")
                    .append("</D:response>\n");

            // 遍历子项
            for (FileInfo file : files) {
                String fileName = file.getFileName();
                boolean isDir = file.isDir();
                long size = file.getFullSize();
                long modifiedTime = file.getUploadTime();

                // 構造子項的相對路徑
                String relativeFilePath = path.endsWith("/") ? path + fileName : path + "/" + fileName;

                // ★★★ Neko醬的修改點 ② ★★★
                // 為每個子項生成正確的、帶有前綴的href
                String fileHref = CONTEXT_PATH + relativeFilePath;
                Instant modifiedInstant = Instant.ofEpochSecond(modifiedTime);
                String lastModifiedStr = RFC1123_FORMATTER.format(modifiedInstant);

                xmlBuilder.append("<D:response>\n")
                        .append("<D:href>").append(escapeXml(fileHref)).append("</D:href>\n") // 使用修正後的路徑
                        .append("<D:propstat>\n")
                        .append("<D:prop>\n")
                        .append("<D:displayname>").append(escapeXml(fileName)).append("</D:displayname>\n")
                        .append("<D:getlastmodified>").append(lastModifiedStr).append("</D:getlastmodified>\n");

                if (isDir) {
                    xmlBuilder.append("<D:resourcetype><D:collection/></D:resourcetype>\n");
                } else {
                    xmlBuilder.append("<D:resourcetype/>\n");
                    xmlBuilder.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>\n");
                }

                xmlBuilder.append("</D:prop>\n")
                        .append("<D:status>HTTP/1.1 200 OK</D:status>\n")
                        .append("</D:propstat>\n")
                        .append("</D:response>\n");
            }

            xmlBuilder.append("</D:multistatus>");
            response.getWriter().write(xmlBuilder.toString());
        } catch (Exception e) {
            log.error("PROPFIND请求处理失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * XML转义方法
     */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String getDisplayName(String path) {
        return path.substring(path.lastIndexOf('/'));
    }

    private String getTargetPath(HttpServletRequest request, String target, boolean dir) {
        String prefix = StringUtil.getPrefix(request);
        target =  target.substring((prefix + "/webdav").length());
        if (dir) {
            target = target + "/";
        }
        return target;
    }
}
