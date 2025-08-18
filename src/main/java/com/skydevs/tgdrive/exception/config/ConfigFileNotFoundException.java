package com.skydevs.tgdrive.exception.config;

import com.skydevs.tgdrive.exception.BaseException;

public class ConfigFileNotFoundException extends BaseException {
    public ConfigFileNotFoundException() {
        super("配置文件不存在");
    }

    public ConfigFileNotFoundException(String msg) {
        super(msg);
    }
}
