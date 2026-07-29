package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.WebDavConfig;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.WebDavConfigService;
import com.skydevs.tgdrive.service.WebDavFileService;
import com.skydevs.tgdrive.service.WebDavService;
import com.skydevs.tgdrive.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

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
    private final WebDavConfigService webDavConfigService;

    private static final String CONTEXT_PATH = "/webdav";
    private static final DateTimeFormatter RFC1123_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    @Override
    public void switchMethod(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String realMethod = (String) request.getAttribute("X-HTTP-Method-Override");
        String realURI = request.getRequestURI().substring((CONTEXT_PATH + "/dispatch").length());
        // URL 解码：getRequestURI() 返回未解码路径，中文等特殊字符以 %xx 编码
        try {
            realURI = UriUtils.decode(realURI, "UTF-8");
        } catch (Exception e) {
            log.warn("WebDAV 路径解码失败: {}", realURI);
        }
        // 规范化：空路径视为根
        if (realURI.isEmpty()) {
            realURI = "/";
        }
        log.info("WebDAV {} {}", realMethod, realURI);

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
            case "LOCK":
                handleLock(request, response, realURI);
                break;
            case "UNLOCK":
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unsupported WebDAV method");
                break;
        }
    }

    /**
     * 处理 PROPPATCH：服务器不支持修改属性，但返回成功以兼容客户端
     */
    private void handlePropPatch(HttpServletRequest request, HttpServletResponse response, String realURI) throws IOException {
        response.setStatus(207);
        response.setContentType("application/xml;charset=UTF-8");
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:multistatus xmlns:D=\"DAV:\">" +
                "<D:response>" +
                "<D:href>" + escapeXml(CONTEXT_PATH + realURI) + "</D:href>" +
                "<D:propstat><D:status>HTTP/1.1 200 OK</D:status></D:propstat>" +
                "</D:response>" +
                "</D:multistatus>";
        response.getWriter().write(xmlResponse);
    }

    /**
     * 处理 LOCK：返回一个最小化锁令牌，兼容需要锁定才能写入的客户端（如 Windows）
     */
    private void handleLock(HttpServletRequest request, HttpServletResponse response, String realURI) throws IOException {
        String token = "opaquelocktoken:" + java.util.UUID.randomUUID();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/xml;charset=UTF-8");
        response.setHeader("Lock-Token", "<" + token + ">");
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:prop xmlns:D=\"DAV:\">" +
                "<D:lockdiscovery><D:activelock>" +
                "<D:locktype><D:write/></D:locktype>" +
                "<D:lockscope><D:exclusive/></D:lockscope>" +
                "<D:depth>infinity</D:depth>" +
                "<D:timeout>Second-3600</D:timeout>" +
                "<D:locktoken><D:href>" + token + "</D:href></D:locktoken>" +
                "<D:lockroot><D:href>" + escapeXml(CONTEXT_PATH + realURI) + "</D:href></D:lockroot>" +
                "</D:activelock></D:lockdiscovery>" +
                "</D:prop>";
        response.getWriter().write(xml);
    }

    /**
     * 处理 MOVE
     */
    private void handleMove(HttpServletRequest request, HttpServletResponse response, String realURI) {
        if (!isOperationAllowed(c -> c.getAllowMove())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        FileInfo sourceFile = locate(realURI);
        if (sourceFile == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String destination = request.getHeader("Destination");
        if (destination == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String srcPath = sourceFile.getWebdavPath();
        boolean isDir = sourceFile.isDir();
        String target = getTargetPath(request, destination, isDir);
        if (target == null || target.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (target.equals(srcPath)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        // RFC 4918: Overwrite 默认为 T
        String overwrite = request.getHeader("Overwrite");
        if (overwrite == null || overwrite.isEmpty()) {
            overwrite = "T";
        }
        FileInfo targetFile = locate(target);
        if (targetFile != null && "F".equalsIgnoreCase(overwrite)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT); // 409
            return;
        }
        // 允许覆盖且目标存在：先删除目标（含其子项）
        if (targetFile != null) {
            fileMapper.deleteFileByWebDav(targetFile.getWebdavPath());
        }
        // 整体移动（保留全部属性）
        if (isDir) {
            fileMapper.moveWebdavByPrefix(srcPath, target);
        } else {
            fileMapper.moveWebdavExact(srcPath, target);
        }
        // 同步顶层项的文件名为新路径对应的名称
        fileMapper.updateFileNameByPath(target, displayName(target));
        log.info("MOVE {} -> {}", srcPath, target);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
    }

    /**
     * 处理 COPY
     */
    private void handleCopy(HttpServletRequest request, HttpServletResponse response, String realURI) {
        if (!isOperationAllowed(c -> c.getAllowCopy())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        FileInfo sourceFile = locate(realURI);
        if (sourceFile == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String destination = request.getHeader("Destination");
        if (destination == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String srcPath = sourceFile.getWebdavPath();
        boolean isDir = sourceFile.isDir();
        String target = getTargetPath(request, destination, isDir);
        if (target == null || target.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (target.equals(srcPath)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        String overwrite = request.getHeader("Overwrite");
        if (overwrite == null || overwrite.isEmpty()) {
            overwrite = "T";
        }
        FileInfo targetFile = locate(target);
        if (targetFile != null && "F".equalsIgnoreCase(overwrite)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return;
        }
        if (targetFile != null) {
            fileMapper.deleteFileByWebDav(targetFile.getWebdavPath());
        }
        // 复制（file_id 共享同一 Telegram 文件，属性保留）
        if (isDir) {
            fileMapper.copyWebdavByPrefix(srcPath, target);
        } else {
            fileMapper.copyWebdavExact(srcPath, target);
        }
        // 同步顶层项的文件名为新路径对应的名称（子项的 file_name 仍正确，无需更新）
        fileMapper.updateFileNameByPath(target, displayName(target));
        log.info("COPY {} -> {}", srcPath, target);
        response.setStatus(HttpServletResponse.SC_CREATED); // 201
    }

    /**
     * 处理新建文件夹
     */
    private void handleMkCol(HttpServletRequest request, HttpServletResponse response, String realURI) {
        if (!isOperationAllowed(c -> c.getAllowMkdir())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        // 规范化目录路径：统一以 / 结尾，保证后续 PROPFIND/列表一致
        if (!realURI.endsWith("/")) {
            realURI = realURI + "/";
        }
        FileInfo existing = locate(realURI);
        if (existing != null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED); // 405
            return;
        }
        FileInfo fileInfo = FileInfo.builder()
                .fileId("dir")
                .fileName(StringUtil.getDisplayName(realURI, true))
                .downloadUrl("dir")
                .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                .size("0")
                .fullSize(0L)
                .webdavPath(realURI)
                .dir(true)
                .build();
        fileMapper.insertFile(fileInfo);
        log.info("MKCOL {}", realURI);
        response.setStatus(HttpServletResponse.SC_CREATED); // 201
    }

    /**
     * 处理 PROPFIND（目录探测）
     */
    private void handlePropFind(HttpServletRequest request, HttpServletResponse response, String realURI) throws IOException {
        // 规范化根路径
        if (realURI.isEmpty()) {
            realURI = "/";
        }
        // 查找当前资源（支持无尾斜杠访问目录）
        FileInfo currentItem = locate(realURI);
        if (!realURI.equals("/") && currentItem == null) {
            log.info("PROPFIND 资源不存在: {}", realURI);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 规范化当前路径：目录统一以 / 结尾
        String currentPath = realURI;
        if (currentItem != null && currentItem.isDir() && !currentPath.endsWith("/")) {
            currentPath = currentPath + "/";
        }

        // 解析 Depth 头（默认 1）
        String depthHeader = request.getHeader("Depth");
        int depth = 1;
        if (depthHeader != null) {
            if ("0".equals(depthHeader)) {
                depth = 0;
            } else if ("infinity".equalsIgnoreCase(depthHeader)) {
                depth = 1; // 限制为 1，避免深度递归拖垮性能
            }
        }

        response.setStatus(207);
        response.setContentType("application/xml;charset=UTF-8");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<D:multistatus xmlns:D=\"DAV:\">\n");

        // 当前资源自身
        appendResponse(xml, currentPath, realURI.equals("/") ? null : currentItem);

        // Depth:1 时附加直接子项
        if (depth >= 1 && (realURI.equals("/") || (currentItem != null && currentItem.isDir()))) {
            List<FileInfo> childFiles = webDavFileService.listFiles(currentPath);
            for (FileInfo file : childFiles) {
                // 直接用数据库中的规范 webdav_path 作为 href/displayname 来源，避免 file_name 过时
                String childPath = file.getWebdavPath();
                appendResponse(xml, childPath, file);
            }
        }

        xml.append("</D:multistatus>");
        response.getWriter().write(xml.toString());
    }

    /**
     * 追加单个 response 节点
     * @param path 规范化后的路径（目录以 / 结尾）
     * @param file 文件信息，根目录时为 null
     */
    private void appendResponse(StringBuilder xml, String path, FileInfo file) {
        boolean isCollection = path.equals("/") || (file != null && file.isDir());
        String href = CONTEXT_PATH + path;
        String name = displayName(path);

        xml.append("<D:response>\n")
                .append("<D:href>").append(escapeXml(href)).append("</D:href>\n")
                .append("<D:propstat>\n")
                .append("<D:prop>\n")
                .append("<D:displayname>").append(escapeXml(name)).append("</D:displayname>\n");

        long modifiedTime = (file == null) ? Instant.now().getEpochSecond() : file.getUploadTime();
        xml.append("<D:getlastmodified>").append(RFC1123_FORMATTER.format(Instant.ofEpochSecond(modifiedTime))).append("</D:getlastmodified>\n");

        if (isCollection) {
            xml.append("<D:resourcetype><D:collection/></D:resourcetype>\n");
        } else {
            xml.append("<D:resourcetype/>\n");
            long size = file != null && file.getFullSize() != null ? file.getFullSize() : 0L;
            xml.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>\n");
            xml.append("<D:getcontenttype>application/octet-stream</D:getcontenttype>\n");
        }

        xml.append("</D:prop>\n")
                .append("<D:status>HTTP/1.1 200 OK</D:status>\n")
                .append("</D:propstat>\n")
                .append("</D:response>\n");
    }

    /**
     * 计算显示名：去掉尾部斜杠后取最后一段；根目录返回空字符串
     */
    private String displayName(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        String name = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int idx = name.lastIndexOf('/');
        return name.substring(idx + 1);
    }

    /**
     * XML 转义
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

    /**
     * 解析 Destination 头为目标 webdav 路径，并 URL 解码；目录补尾斜杠
     */
    private String getTargetPath(HttpServletRequest request, String target, boolean dir) {
        String prefix = StringUtil.getPrefix(request);
        String base = prefix + CONTEXT_PATH;
        if (!target.startsWith(base)) {
            return null;
        }
        String path = target.substring(base.length());
        try {
            path = UriUtils.decode(path, "UTF-8");
        } catch (Exception e) {
            log.warn("Destination 解码失败: {}", target);
        }
        if (path.isEmpty()) {
            path = "/";
        }
        if (dir && !path.endsWith("/")) {
            path = path + "/";
        }
        return path;
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
     * 定位 WebDAV 资源：先精确匹配，再尝试 URL 解码，再尝试无尾斜杠→有尾斜杠（目录），再大小写不敏感
     * 返回的 FileInfo.webdavPath 为数据库中的规范路径
     */
    private FileInfo locate(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return null;
        }
        FileInfo file = fileMapper.getFileByWebdavPath(path);
        if (file != null) {
            return file;
        }
        // URL 解码
        try {
            String decoded = UriUtils.decode(path, "UTF-8");
            if (!decoded.equals(path)) {
                file = fileMapper.getFileByWebdavPath(decoded);
                if (file != null) {
                    return file;
                }
            }
        } catch (Exception ignored) {
        }
        // 目录：无尾斜杠 → 有尾斜杠
        if (!path.endsWith("/")) {
            file = fileMapper.getFileByWebdavPath(path + "/");
            if (file != null) {
                return file;
            }
        }
        // 大小写不敏感
        try {
            String parent = path.substring(0, path.lastIndexOf('/') + 1);
            List<FileInfo> siblings = fileMapper.getFilesByPathPrefix(parent);
            for (FileInfo f : siblings) {
                if (f.getWebdavPath().equalsIgnoreCase(path) || f.getWebdavPath().equalsIgnoreCase(path + "/")) {
                    return f;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
