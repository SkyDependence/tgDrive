package com.skydevs.tgdrive.exception;

/**
 * 业务异常
 */
public class BaseException extends RuntimeException {
    //TODO: 分类整理Exception类型

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }

}
