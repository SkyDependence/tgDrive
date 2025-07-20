package com.skydevs.tgdrive.exception;

public class NoConfigException extends BaseException{
    public NoConfigException() {
        super("当前没有配置文件");
    }

    public NoConfigException(String msg) {
        super(msg);
    }
}
