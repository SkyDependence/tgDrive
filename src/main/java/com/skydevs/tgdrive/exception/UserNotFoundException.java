package com.skydevs.tgdrive.exception;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super("用户不存在");
    }

    public UserNotFoundException(String msg) {
        super(msg);
    }
}
