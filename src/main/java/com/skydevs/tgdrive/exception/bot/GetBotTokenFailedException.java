package com.skydevs.tgdrive.exception.bot;

import com.skydevs.tgdrive.exception.BaseException;

public class GetBotTokenFailedException extends BaseException {
    public GetBotTokenFailedException() {
        super("获取bot token失败");
    }

    public GetBotTokenFailedException(String msg) {
        super(msg);
    }
}
