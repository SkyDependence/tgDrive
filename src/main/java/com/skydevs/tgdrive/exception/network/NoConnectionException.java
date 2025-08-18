package com.skydevs.tgdrive.exception.network;

import com.skydevs.tgdrive.exception.BaseException;

public class NoConnectionException extends BaseException {
    public NoConnectionException() {
        super("请检查网络连接");
    }
    public NoConnectionException(String msg) {
        super(msg);
    }
}
