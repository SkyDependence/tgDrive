package com.skydevs.tgdrive.exception.user;

import com.skydevs.tgdrive.exception.BaseException;

public class PasswordValidationException extends BaseException {
    public PasswordValidationException() {
        super("密码验证失败");
    }

    public PasswordValidationException(String message) {
        super(message);
    }
}