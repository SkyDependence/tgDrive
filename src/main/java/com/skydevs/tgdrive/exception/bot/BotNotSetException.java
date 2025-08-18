package com.skydevs.tgdrive.exception.bot;

import com.skydevs.tgdrive.exception.BaseException;

public class BotNotSetException extends BaseException {
    public BotNotSetException() {
        super("bot token未设置");
    }

    public BotNotSetException(String msg) {
        super(msg);
    }
}
