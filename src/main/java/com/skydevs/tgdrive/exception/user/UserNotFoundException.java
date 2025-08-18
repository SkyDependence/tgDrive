package com.skydevs.tgdrive.exception.user;

import com.skydevs.tgdrive.exception.BaseException;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super("用户不存在");
    }

    public UserNotFoundException(String msg) {
        super(msg);
    }
}
