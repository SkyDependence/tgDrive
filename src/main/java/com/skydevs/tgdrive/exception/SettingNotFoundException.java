package com.skydevs.tgdrive.exception;

public class SettingNotFoundException extends BaseException{
    public SettingNotFoundException() {
        super("该设置不存在");
    }

    public SettingNotFoundException(String msg) {
        super(msg);
    }
}
