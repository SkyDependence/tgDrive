package com.skydevs.tgdrive.exception;

public class BotNotSetException extends BaseException{
    public BotNotSetException() {
        super("bot token未设置");
    }

    public BotNotSetException(String msg) {
        super(msg);
    }
}
