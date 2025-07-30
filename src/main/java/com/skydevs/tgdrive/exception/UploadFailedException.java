package com.skydevs.tgdrive.exception;

public class UploadFailedException extends BaseException{
    public UploadFailedException() {
        super("上传文件失败！");
    }

    public UploadFailedException(String msg) {
        super(msg);
    }
}
