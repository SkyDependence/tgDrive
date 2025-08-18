package com.skydevs.tgdrive.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.skydevs.tgdrive.exception.BaseException;
import com.skydevs.tgdrive.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     * @param ex 业务异常
     * @return 返回异常信息
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex) {
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

    //TODO: IDM异常中止
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

    // 拦截：无此角色异常
    @ExceptionHandler(NotRoleException.class)
    public Result<String> handlerException(NotRoleException e) {
        log.warn("访问被拒绝 -> 缺少角色: {}", e.getRole());
        return Result.error("非admin，权限不足");
    }

    // 拦截：无此权限异常
    @ExceptionHandler(NotPermissionException.class)
    public Result<String> handlerException(NotPermissionException e) {
        log.warn("访问被拒绝 -> 缺少权限: {}", e.getPermission());
        return Result.error("无此权限，禁止访问");
    }

    // 拦截：未登录异常
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotLoginException.class)
    public Result<String> handlerException(NotLoginException e) {
        log.warn("访问被拒绝 -> 原因: {}", e.getMessage());
        return Result.error("请先登录后再访问");
    }

    // 参数校验
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getAllValidationResults().stream()
                .map(validationResult -> validationResult.getResolvableErrors().get(0).getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Controller参数校验失败: {}", message);
        return Result.error(message);
    }
}


