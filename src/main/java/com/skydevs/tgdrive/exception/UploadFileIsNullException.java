package com.skydevs.tgdrive.exception;

public class UploadFileIsNullException extends BaseException{
    public UploadFileIsNullException() {
        super("上传的文件为空！");
    }

    public UploadFileIsNullException(String msg) {
        super(msg);
    }
}
