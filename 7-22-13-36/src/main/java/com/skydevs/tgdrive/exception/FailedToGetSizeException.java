package com.skydevs.tgdrive.exception;

public class FailedToGetSizeException extends BaseException {
    public FailedToGetSizeException() {
        super("无法获取文件大小");
    }

    public FailedToGetSizeException(String msg) {
        super(msg);
    }
}
