package com.skydevs.tgdrive.utils;

import com.pengrad.telegrambot.model.Message;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    /**
     * 获取前缀
     * @param request HTTP请求
     * @return 前缀
     */
    public static String getPrefix(HttpServletRequest request) {
        String protocol = request.getHeader("X-Forwarded-Proto") != null ? request.getHeader("X-Forwarded-Proto") : request.getScheme(); // 先代理请求头中获取协议
        String host = request.getServerName(); // 获取主机名 localhost 或实际域名
        int port = request.getHeader("X-Forwarded-Port") != null ? Integer.parseInt(request.getHeader("X-Forwarded-Port")) : request.getServerPort(); // 先从代理请求头中获取端口号 80 或其他
        // 如果是默认端口，则省略端口号
        if ((protocol.equalsIgnoreCase("http") && port == 80) || (protocol.equalsIgnoreCase("https") && port == 443)) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + port;
    }

    /**
     * 获取相对路径
     * @param path 路径
     * @return 相对路径
     */
    public static String getPath(String path) {
        return path.substring("/webdav".length());
    }

    /**
     * 获取纯文件名
     * @param path 相对路径
     * @param dir 是否为文件夹
     * @return 文件名
     */
    public static String getDisplayName(String path, boolean dir) {
        if (dir) {
            path = path.substring(0, path.lastIndexOf('/'));
            path = path.substring(path.lastIndexOf('/') + 1);
            return path;
        } else {
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    /**
     * 获取路径中需要确保存在的所有上级目录路径
     * 仅用于文件上传场景：路径最后一段视为文件名，跳过；前面所有非空段都是上级目录。
     * 不再因段名包含 "." 而中断，以支持 "v1.2" 这类带点的目录名。
     * @param path 文件路径
     * @return 各级目录路径列表（每个以 / 结尾）
     */
    public static List<String> getDirsPathFromPath(String path) {
        String[] paths = path.split("/");
        List<String> dirPaths = new ArrayList<>();
        StringBuilder currentPath = new StringBuilder();
        // 最后一段是文件名，跳过；前面所有非空段都是需要确保存在的目录
        for (int i = 0; i < paths.length - 1; i++) {
            String p = paths[i];
            if (p.isEmpty()) {
                continue;
            }
            currentPath.append("/").append(p);
            dirPaths.add(currentPath.toString() + "/");
        }
        return dirPaths;
    }

    /**
     * 从消息中提取文件ID
     * @param message Telegram消息
     * @return 文件ID
     */
    public static String extractFileId(Message message) {
        if (message == null) {
            return null;
        }

        // 按优先级检查可能的文件类型
        if (message.document() != null) {
            return message.document().fileId();
        } else if (message.sticker() != null) {
            return message.sticker().fileId();
        } else if (message.video() != null) {
            return message.video().fileId();
        } else if (message.photo() != null && message.photo().length > 0) {
            return message.photo()[message.photo().length - 1].fileId(); // 取最后一张（通常是最高分辨率）
        } else if (message.audio() != null) {
            return message.audio().fileId();
        } else if (message.animation() != null) {
            return message.animation().fileId();
        } else if (message.voice() != null) {
            return message.voice().fileId();
        } else if (message.videoNote() != null) {
            return message.videoNote().fileId();
        }

        return null; // 没有找到 fileId
    }

}
