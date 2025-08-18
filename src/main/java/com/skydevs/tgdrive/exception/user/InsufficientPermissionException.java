package com.skydevs.tgdrive.exception.user;

import com.skydevs.tgdrive.exception.BaseException;

public class InsufficientPermissionException extends BaseException {
    public InsufficientPermissionException() {
        super("权限不足");
    }

    public InsufficientPermissionException(String message) {
        super(message);
    }
}