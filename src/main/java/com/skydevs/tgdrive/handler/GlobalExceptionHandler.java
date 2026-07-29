package com.skydevs.tgdrive.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.skydevs.tgdrive.exception.BaseException;
import com.skydevs.tgdrive.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<String> handlerException(NotRoleException e) {
        log.warn("访问被拒绝 -> 缺少角色: {}", e.getRole());
        return Result.error("非admin，权限不足");
    }

    // 拦截：无此权限异常
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
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

    // 参数校验（@RequestParam / @PathVariable 校验失败）
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getAllValidationResults().stream()
                .map(validationResult -> validationResult.getResolvableErrors().get(0).getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Controller参数校验失败: {}", message);
        return Result.error(message);
    }

    // 参数校验（@RequestBody @Valid 校验失败）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("请求体参数校验失败: {}", message);
        return Result.error(message);
    }

    // 参数类型转换失败（如 page=abc）
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败: {}", e.getMessage());
        return Result.error("参数类型错误: " + e.getName());
    }

    // 缺少必填请求参数
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return Result.error("缺少必填参数: " + e.getParameterName());
    }

    // 请求体 JSON 解析失败
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error("请求体格式错误，请检查 JSON");
    }

    // Spring Boot 3.x：静态资源未找到时抛出 NoResourceFoundException，应返回 404 而非 500
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.error("资源不存在");
    }

    /**
     * 通用异常兜底：捕获所有未处理的异常，避免向客户端泄露内部堆栈或敏感信息
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("服务器内部错误，请稍后重试");
    }
}


