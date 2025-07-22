package com.skydevs.tgdrive.handler;

import com.skydevs.tgdrive.exception.BaseException;
import com.skydevs.tgdrive.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     * @param ex 业务异常
     * @return 返回异常信息
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 客户端终止连接处理
     * @param e 客户端终止连接异常
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        // 客户端中止连接，记录为信息级别日志或忽略
        log.info("客户端中止了连接：{}", e.getMessage());
    }

    /**
     * 客户端终止连接处理
     * @param e 客户端终止连接异常
     */
    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        String message = e.getMessage();
        if (message != null && (message.contains("An established connection was aborted") || message.contains("你的主机中的软件中止了一个已建立的连接"))) {
            log.info("客户端中止了连接：{}", message);
        } else {
            // 处理其他 IOException
            log.error("发生了 IOException", e);
        }
    }
}


