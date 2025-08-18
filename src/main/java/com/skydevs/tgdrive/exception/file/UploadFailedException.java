package com.skydevs.tgdrive.exception.file;

import com.skydevs.tgdrive.exception.BaseException;

public class UploadFailedException extends BaseException {
    public UploadFailedException() {
        super("上传文件失败！");
    }

    public UploadFailedException(String msg) {
        super(msg);
    }
}
