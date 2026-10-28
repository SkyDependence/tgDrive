package com.skydevs.tgdrive.service.impl;

import com.alibaba.fastjson.JSON;
import com.pengrad.telegrambot.model.File;
import com.skydevs.tgdrive.entity.BigFileInfo;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.bot.BotNotSetException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.DownloadService;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.MediaType;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private final FileStorageService fileStorageService;
    private final TelegramBotService telegramBotService;
    private final FileMapper fileMapper;

    // 优化的HTTP客户端配置
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            // 单块最大 10MB，跨境访问 Telegram CDN 时读取可能较慢；readTimeout 是两次读之间的空闲上限，
            // 调大到 5 分钟以抵抗慢链路，避免正常传输中被误判超时中断（配合分块级重试）
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            // 连接池：适当放大空闲连接数，多用户/多分块下载时减少频繁建连
            .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
            // 启用连接复用与连接失败自动重试
            .retryOnConnectionFailure(true)
            .build();

    /**
     * 下载文件
     * @param fileID
     * @return
     */
    // 用于 MIME 检测的头部字节数（普通文件流式直传时，用这段头部做类型探测，无需整文件）。
    private static final int MIME_PROBE_SIZE = 64 * 1024;
    // 疑似 record 索引（以 '{' 开头）时，最多再额外读取多少字节用于解析。
    // record 索引为 JSON 文本，每个分片 fileId 约 90 字节，8MB 可容纳约 9 万个分片，
    // 远超任何实际大文件的分片数，彻底避免「分片过多导致 record JSON 被截断而误判为普通文件」。
    // 该内存仅在「内容以 '{' 开头」时才可能分配（普通二进制文件不会走此分支）。
    private static final int RECORD_PROBE_LIMIT = 8 * 1024 * 1024;

    // 当前请求是否为「预览」模式（inline）。仅在构建响应头的同一线程内有效，
    // 响应体流式写出发生在另一线程，但那时 headers 已构建完毕，不依赖该标志。
    private static final ThreadLocal<Boolean> INLINE_FLAG = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public ResponseEntity<StreamingResponseBody> downloadFile(String fileID) {
        return downloadFile(fileID, false);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadFile(String fileID, boolean inline) {
        INLINE_FLAG.set(inline);
        try {
            return doDownloadFile(fileID);
        } finally {
            INLINE_FLAG.remove();
        }
    }

    private ResponseEntity<StreamingResponseBody> doDownloadFile(String fileID) {
        InputStream inputStream = null;
        try {
            inputStream = downloadFileInputStream(fileID);

            // 先读头部 MIME_PROBE_SIZE 字节：既用于 MIME 检测，也用于判断首字节是否为 '{'。
            // 普通文件到此仅占用一段头部内存（≤64KB），随后与剩余流拼接流式直传。
            byte[] head = new byte[MIME_PROBE_SIZE];
            int headLen = 0;
            int r;
            while (headLen < MIME_PROBE_SIZE
                    && (r = inputStream.read(head, headLen, MIME_PROBE_SIZE - headLen)) != -1) {
                headLen += r;
            }
            final byte[] headBytes = (headLen == MIME_PROBE_SIZE) ? head : Arrays.copyOf(head, headLen);

            BigFileInfo record = null;
            // 仅当内容以 '{' 开头（疑似 record JSON）时才尝试解析。
            // 普通二进制文件几乎不以 '{' 开头，直接走流式，零额外读取。
            if (looksLikeJsonObject(headBytes)) {
                if (headLen < MIME_PROBE_SIZE) {
                    // 整个文件已在头部内（小文件），直接解析
                    try (InputStream hs = new ByteArrayInputStream(headBytes)) {
                        record = parseBigFileInfo(hs);
                    }
                } else {
                    // 头部装不下：继续读取（上限 RECORD_PROBE_LIMIT）以获取完整候选 JSON。
                    // 已读字节保留在 collected 中，无论是否 record 都不会丢失（后续用于拼接）。
                    ByteArrayOutputStream collected = new ByteArrayOutputStream(MIME_PROBE_SIZE * 2);
                    collected.write(headBytes, 0, headBytes.length);
                    byte[] tmp = new byte[64 * 1024];
                    boolean overflow = false;
                    int rr;
                    while (collected.size() < RECORD_PROBE_LIMIT
                            && (rr = inputStream.read(tmp)) != -1) {
                        collected.write(tmp, 0, rr);
                    }
                    // 判断是否还有剩余（超出上限 → 不是 record 索引，是大 JSON 普通文件）
                    int nextByte = -1;
                    if (collected.size() >= RECORD_PROBE_LIMIT) {
                        nextByte = inputStream.read();
                        overflow = (nextByte != -1);
                    }
                    byte[] collectedBytes = collected.toByteArray();
                    if (!overflow) {
                        try (InputStream hs = new ByteArrayInputStream(collectedBytes)) {
                            record = parseBigFileInfo(hs);
                        }
                    } else {
                        // 极低概率：内容以 '{' 开头且超过 8MB 仍未结束。若其疑似 record 索引（含特征字段），
                        // 说明分片数超出探测上限，此时不应静默当普通文件下载，记录告警便于排查。
                        String prefix = new String(collectedBytes, 0,
                                Math.min(collectedBytes.length, 4096), StandardCharsets.UTF_8);
                        if (prefix.contains("\"recordFile\"") || prefix.contains("\"fileIds\"")) {
                            log.warn("检测到疑似超大 record 索引（>{}MB），已超出解析上限，fileID={}，将按普通文件处理，请关注",
                                    RECORD_PROBE_LIMIT / (1024 * 1024), fileID);
                        }
                    }
                    if (record == null || !record.isRecordFile()) {
                        // 普通文件：把已读取的全部字节 +（可能已读出的 nextByte）+ 剩余流拼接后流式直传
                        InputStream headStream = new ByteArrayInputStream(collectedBytes);
                        InputStream tail = (nextByte != -1)
                                ? new SequenceInputStream(
                                        new ByteArrayInputStream(new byte[]{(byte) nextByte}), inputStream)
                                : inputStream;
                        InputStream combined = new SequenceInputStream(headStream, tail);
                        inputStream = null; // 所有权转移
                        return handleRegularFile(fileID, combined, collectedBytes);
                    }
                }
            }

            if (record != null && record.isRecordFile()) {
                // record 文件：索引已解析，原始流不再需要，关闭后走分片流式下载
                inputStream.close();
                inputStream = null;
                return handleRecordFile(fileID, record);
            }

            // 普通文件（含不以 '{' 开头、或以 '{' 开头但小文件解析非 record）：
            // 把「已读头部」与「剩余流」拼接后流式直传，避免整文件入内存
            InputStream combined = new SequenceInputStream(
                    new ByteArrayInputStream(headBytes), inputStream);
            inputStream = null; // 所有权转移给 handleRegularFile，由其负责关闭
            return handleRegularFile(fileID, combined, headBytes);
        } catch (IOException e) {
            closeQuietly(inputStream);
            log.error("下载文件失败：" + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (BotNotSetException e) {
            closeQuietly(inputStream);
            throw e;
        } catch (NullPointerException e) {
            closeQuietly(inputStream);
            // 不再无差别吞掉 NPE，记录真实堆栈便于排查
            log.error("下载文件时发生空指针异常，fileID: {}", fileID, e);
            throw new BotNotSetException("下载文件时配置异常，请检查 Bot 配置");
        }
    }

    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 判断字节内容是否疑似 JSON 对象（首个非空白字符为 '{'）。
     * record 索引文件为 JSON 文本，据此快速筛除绝大多数二进制普通文件，避免多余读取。
     */
    private boolean looksLikeJsonObject(byte[] bytes) {
        for (byte b : bytes) {
            // 跳过前导空白（空格/制表/换行/回车）以及可能的 UTF-8 BOM 字节
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r'
                    || b == (byte) 0xEF || b == (byte) 0xBB || b == (byte) 0xBF) {
                continue;
            }
            return b == '{';
        }
        return false;
    }

    /**
     * 校验下载权限
     * 下载链接采用「持有即授权」模型：fileID 为 Telegram 返回的不可枚举、不可预测的随机串，
     * 等同于分享凭证。任何持有下载链接者均可下载，不再要求登录或属主校验。
     * 访问控制的边界放在「文件列表/枚举」等接口（需鉴权，防越权获取 fileID 清单），
     * 而非下载动作本身，从而支持敏感文件的直链分享。
     * @param fileID 文件ID
     */
    @Override
    public void checkDownloadPermission(String fileID) {
        // 仅校验文件是否存在，避免对无效 fileID 触发后续异常；不做登录/属主校验
        FileInfo file = fileMapper.getFileByFileId(fileID);
        if (file == null) {
            throw new com.skydevs.tgdrive.exception.BaseException("文件不存在");
        }
    }

    /**
     * 处理小文件
     * @param fileID
     * @param inputStream
     * @return
     */
    private ResponseEntity<StreamingResponseBody> handleRegularFile(String fileID, InputStream inputStream, byte[] chunkData) {
        log.info("文件不是记录文件，直接下载文件...");

        try {
            File file = telegramBotService.getFile(fileID);
            String filename = resolveFilename(fileID, file.filePath());
            if (filename.lastIndexOf('.') == -1) {
                Tika tika = new Tika();
                // 用头部字节做 MIME 检测（足够识别常见类型），避免依赖整文件
                try (InputStream is = new ByteArrayInputStream(chunkData)) {
                    String mimeType = tika.detect(is);

                    String extension = getExtensionByMimeType(mimeType);
                    if (!extension.isEmpty()) {
                        filename = filename + extension;
                    } else {
                        log.error("未添加扩展名，扩展名检测失败");
                    }
                } catch (Exception e) {
                    log.error("文件检测失败：{}", e.getMessage());
                }
            }
            long fullSize = file.fileSize();

            HttpHeaders headers = setHeaders(filename, fullSize);

            // 流式直传：从「头部+剩余流」拼接的流边读边写给客户端，不整文件入内存
            StreamingResponseBody streamingResponseBody = outputStream -> {
                streamData(inputStream, outputStream);
            };

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(getContentTypeFromFilename(filename)))
                    .body(streamingResponseBody);
        } catch (RuntimeException e) {
            // 构建响应阶段（如 getFile 失败）出错时，及时关闭流避免连接泄漏
            closeQuietly(inputStream);
            throw e;
        }
    }

    private String getExtensionByMimeType(String mimeType) {
        try {
            // 使用Tika的MimeType工具获取扩展名
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType type = allTypes.forName(mimeType);
            return type.getExtension();
        } catch (Exception e) {
            log.error("无法获取扩展名");
            return "";
        }
    }

    /**
     * 流数据处理
     * @param inputStream
     * @param outputStream
     */
    private void streamData(InputStream inputStream, OutputStream outputStream) {
        try (InputStream is = inputStream) {
            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, byteRead);
            }
        } catch (IOException e) {
            handleClientAbortException(e);
        } catch (Exception e) {
            log.info("文件下载终止");
            log.info(e.getMessage(), e);
        }
    }

    /**
     * 处理大文件
     * @param fileID
     * @param record
     * @return
     */
    private ResponseEntity<StreamingResponseBody> handleRecordFile(String fileID, BigFileInfo record) {
        log.info("文件名为：" + record.getFileName());
        log.info("检测到记录文件，开始下载并合并分片文件...");

        String filename = resolveFilename(fileID, record.getFileName());
        Long fullSize = fileMapper.getFullSizeByFileId(fileID);

        HttpHeaders headers = setHeaders(filename, fullSize);

        List<String> partFileIds = record.getFileIds();

        StreamingResponseBody streamingResponseBody = outputStream -> {
            downloadAndMergeFileParts(partFileIds, outputStream);
        };

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(getContentTypeFromFilename(filename)))
                .body(streamingResponseBody);
    }

    /**
     * 下载并合并分片文件
     * @param partFileIds
     * @param outputStream
     */
    private void downloadAndMergeFileParts(List<String> partFileIds, OutputStream outputStream) {
        // 逐块顺序下载 + 单块重试 + 失败即中断：
        //  1. 顺序下载保证拼接正确，避免此前「预建 N 个管道 + N 个任务挤 3 线程 + 1KB 管道缓冲」的阻塞与低吞吐；
        //  2. 每块失败重试（指数退避），抵抗 Telegram/网络抖动；
        //  3. 某块最终失败则抛异常中断响应，避免向客户端返回 HTTP 200 的截断损坏文件（数据完整性）。
        if (partFileIds == null || partFileIds.isEmpty()) {
            log.error("记录文件的分片列表为空，无法合并下载");
            throw new RuntimeException("文件分片信息缺失，无法下载");
        }
        for (int i = 0; i < partFileIds.size(); i++) {
            String partFileId = partFileIds.get(i);
            try {
                downloadSinglePartWithRetry(partFileId, i, partFileIds.size(), outputStream);
            } catch (IOException e) {
                // 客户端主动断开：连接已不可用，立即停止后续分片下载，避免对已断连接反复请求 Telegram、做无用功
                if (isClientAbort(e)) {
                    log.info("客户端中止了连接，停止后续分片下载（已完成 {}/{} 块）：{}", i, partFileIds.size(), e.getMessage());
                    return;
                }
                // 服务端下载失败：上抛中断响应，让客户端感知文件不完整（handleClientAbortException 会包成 RuntimeException 抛出）
                handleClientAbortException(e);
            }
        }
    }

    /**
     * 下载单个分片并写入输出流，带重试。
     * 关键正确性约束：一旦本块已向 outputStream 写出过字节，就不能再重试整块（否则会产生「半块 + 完整块」的重复数据）。
     * 因此仅在「本次尝试尚未写出任何字节前，读取 Telegram 分片失败」时才重试；
     * 已开始写出后发生的任何错误（无论是 Telegram 读错误还是客户端写错误）一律直接上抛，避免数据重复/错位。
     */
    private void downloadSinglePartWithRetry(String partFileId, int index, int total, OutputStream outputStream) throws IOException {
        int maxRetry = 3;
        int baseDelayMs = 1000;
        IOException lastError = null;

        for (int attempt = 0; attempt < maxRetry; attempt++) {
            Response response = null;
            boolean writtenThisAttempt = false; // 本次尝试是否已向客户端写出字节
            try {
                response = downloadFileByte(partFileId);
                try (InputStream partInputStream = response.body().byteStream()) {
                    byte[] buffer = new byte[64 * 1024];
                    int bytesRead;
                    while ((bytesRead = partInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        writtenThisAttempt = true;
                    }
                    outputStream.flush();
                }
                return; // 本块成功
            } catch (IOException e) {
                lastError = e;
                // 已经写出过字节：无法安全重试（会导致重复数据），直接上抛中断整个下载
                if (writtenThisAttempt) {
                    log.error("分片下载中途失败且已写出部分数据，无法重试，fileId={}（第 {}/{} 块）", partFileId, index + 1, total);
                    throw e;
                }
                // 客户端断开：不重试，直接上抛（上层 handleClientAbortException 会识别为正常终止）
                if (isClientAbort(e)) {
                    throw e;
                }
                // 尚未写出任何字节的读取失败：可安全重试整块
                log.warn("分片下载失败（未写出数据，可重试），fileId={}（第 {}/{} 块），第 {} 次重试", partFileId, index + 1, total, attempt + 1);
                if (attempt < maxRetry - 1) {
                    try {
                        Thread.sleep((long) baseDelayMs * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("分片下载重试被中断", ie);
                    }
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
        // 重试耗尽仍失败：抛出，避免返回损坏文件
        log.error("分片下载最终失败，fileId={}（第 {}/{} 块），已重试 {} 次", partFileId, index + 1, total, maxRetry);
        throw new IOException("分片下载失败，文件不完整：" + partFileId, lastError);
    }

    /**
     * 判断某个 IOException 是否由「客户端主动断开连接」引起（如用户取消下载、关闭页面）。
     * 统一各处判定关键词，避免不同位置识别不一致导致「正常断开被误判为服务端错误」。
     */
    private boolean isClientAbort(Throwable e) {
        if (e == null) {
            return false;
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("An established connection was aborted")
                || message.contains("你的主机中的软件中止了一个已建立的连接")
                || message.contains("Broken pipe")
                || message.contains("Connection reset")
                || message.contains("aborted")
                || message.contains("中止");
    }

    /**
     * 处理客户端终止连接异常：客户端断开视为正常终止（仅记录），其余 IO 错误上抛。
     * @param e
     */
    private void handleClientAbortException(IOException e) {
        if (isClientAbort(e)) {
            log.info("客户端中止了连接：{}", e.getMessage());
        } else {
            log.error("写入输出流时发生 IOException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理文件名
     * @param fileID
     * @param defaultName
     * @return
     */
    private String resolveFilename(String fileID, String defaultName) {
        String filename = fileMapper.getFileNameByFileId(fileID);
        if (filename == null) {
            filename = defaultName;
        }

        return filename;
    }

    /**
     * 尝试转换为大文件的记录文件
     * @param inputStream 下载的文件的输入流
     * @return BigFilInfo
     */
    private BigFileInfo parseBigFileInfo(InputStream inputStream) {
        try {
            String fileContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(fileContent, BigFileInfo.class);
        } catch (Exception e) {
            log.info("文件不是 BigFileInfo类型，作为普通文件处理");
            return null;
        }
    }

    /**
     * 下载文件并转换为流处理
     * @param fileID
     * @return
     * @throws IOException
     */
    private InputStream downloadFileInputStream(String fileID) throws IOException {
        File file = telegramBotService.getFile(fileID);
        String fileUrl = telegramBotService.getFullFilePath(file);

        Request request = new Request.Builder()
                .url(fileUrl)
                .get()
                .build();

        Response response = okHttpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            log.error("无法下载文件，响应码：" + response.code());
            response.close();
            throw new IOException("无法下载文件，响应码：" + response.code());
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            log.error("响应体为空");
            response.close();
            throw new IOException("响应体为空");
        }

        // 包装流：关闭流时同时关闭 Response，防止连接泄漏
        InputStream rawStream = responseBody.byteStream();
        return new FilterInputStream(rawStream) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    response.close();
                }
            }
        };
    }

    /**
     * 设置响应头
     *
     * @param filename
     * @param size
     * @return
     */
    private HttpHeaders setHeaders(String filename, Long size) {
        HttpHeaders headers = new HttpHeaders();
        try {
            String contentType = getContentTypeFromFilename(filename);
            headers.setContentType(MediaType.parseMediaType(contentType));
            if (size != null && size > 0) {
                headers.setContentLength(size);
            }

            boolean previewMode = Boolean.TRUE.equals(INLINE_FLAG.get());
            if (canInline(contentType, previewMode)) {
                // 可内联渲染的类型：图片/视频始终 inline；PDF/音频/纯文本仅在预览模式下 inline，
                // 以便浏览器（或前端 iframe）内联展示而非直接下载
                headers.setContentDisposition(ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build());
            } else {
                // 使用 URLEncoder 编码文件名，确保支持中文
                String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;
                headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            }
        } catch (UnsupportedEncodingException e) {
            log.error("不支持的编码");
        }
        return headers;
    }

    /**
     * 判断某内容类型是否允许以 inline（内联渲染）方式响应。
     * 安全考量：绝不对 text/html、image/svg+xml 等可执行脚本的类型 inline，
     * 避免用户上传恶意 HTML/SVG 后，他人在本站域下预览触发存储型 XSS。
     *
     * @param contentType 响应内容类型
     * @param previewMode 是否为前端主动请求的预览模式（?preview=1）
     * @return 允许 inline 返回 true
     */
    private boolean canInline(String contentType, boolean previewMode) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        // 明确禁止内联的高风险类型（可能携带并执行脚本）
        if (ct.startsWith("text/html")
                || ct.contains("xhtml")
                || ct.contains("svg")) {
            return false;
        }
        // 图片、视频始终允许内联（历史行为，保持兼容）
        if (ct.startsWith("image/") || ct.startsWith("video/")) {
            return true;
        }
        // 其余类型仅在前端预览模式下内联：PDF、音频、纯文本等
        if (previewMode) {
            return ct.equals("application/pdf")
                    || ct.startsWith("audio/")
                    || ct.startsWith("text/");
        }
        return false;
    }

    /**
     * 下载分片文件
     *
     * @param partFileId
     * @return
     * @throws IOException
     */
    private Response downloadFileByte(String partFileId) throws IOException {
        File partFile = telegramBotService.getFile(partFileId);
        String partFileUrl = telegramBotService.getFullFilePath(partFile);
        Request partRequest = new Request.Builder()
                .url(partFileUrl)
                .get()
                .build();

        Response response = okHttpClient.newCall(partRequest).execute();
        if (!response.isSuccessful()) {
            log.error("无法下载分片文件，响应码：" + response.code());
            response.close();
            throw new IOException("无法下载分片文件，响应码：" + response.code());
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            log.error("分片响应体为空");
            response.close();
            throw new IOException("分片响应体为空");
        }

        return response;
    }

    /**
     * 获取文件类型
     *
     * @param filename
     * @return
     */
    private String getContentTypeFromFilename(String filename) {
        String contentType = null;
        Path path = Paths.get(filename);
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            log.warn("无法通过 Files.probeContentType 获取 MIME 类型: " + e.getMessage());
        }

        if (contentType == null) {
            // 手动映射常见的文件扩展名到 MIME 类型
            String extension = getFileExtension(filename).toLowerCase();
            contentType = switch (extension) {
                case "gif" -> "image/gif";
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "bmp" -> "image/bmp";
                case "txt" -> "text/plain";
                case "pdf" -> "application/pdf";
                case "mp4" -> "video/mp4";
                // 添加其他需要的类型
                default -> "application/octet-stream";
            };
        }
        return contentType;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename
     * @return
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}