package com.skydevs.tgdrive.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class StringUtil {
    /**
     * Description:
     * 获取前缀
     * @author SkyDev
     * @param request HTTP请求
     * @return 前缀
     */
    public static String getPrefix(HttpServletRequest request) {
        String protocol = request.getHeader("X-Forwarded-Proto") != null ?
                request.getHeader("X-Forwarded-Proto") :
                request.getScheme(); // 先代理请求头中获取协议
        log.info("protocol: {}", protocol);
        String host = request.getHeader("Host") != null ?
                request.getHeader("Host").split(":")[0] : // 去除 Host 头中的端口
                request.getServerName(); // 获取主机名 localhost 或实际域名
        log.info("host: {}", host);
        int port = request.getHeader("X-Forwarded-Port") != null ?
                Integer.parseInt(request.getHeader("X-Forwarded-Port")) :
                request.getServerPort(); // 先从代理请求头中获取端口号 80 或其他
        log.info("port: {}", port);
        // 如果是默认端口，则省略端口号
        if ((protocol.equalsIgnoreCase("http") && port == 80) || (protocol.equalsIgnoreCase("https") && port == 443)) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + port;
    }

    /**
     * Description:
     * 获取相对路径
     * @author SkyDev
     * @param path 路径
     * @return 相对路径
     */
    public static String getPath(String path) {
        return path.substring("/webdav".length());
    }

    /**
     * Description:
     * 获取纯文件名
     * @author SkyDev
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
     * Description:
     * 获取路径中的文件夹名字
     * @author SkyDev
     * @param path 路径
     * @return 文件夹名字数组
     */
    public static List<String> getDirsPathFromPath(String path) {
        String[] paths = path.split("/");
        // 去掉文件名
        if (paths.length > 0 && paths[paths.length - 1].contains(".")) {
            paths = Arrays.copyOf(paths, paths.length - 1);
        }

        List<String> dirPaths = new ArrayList<>(); // 用于存储每个文件夹的路径

        StringBuilder currentPath = new StringBuilder(); // 拼接路径

        for (String p : paths) {
            if (p.isEmpty()) {
                continue;
            }
            if (p.contains(".")) {
                break;
            }
            currentPath.append("/" + p);
            dirPaths.add(currentPath + "/");
        }
        return dirPaths;
    }

}
