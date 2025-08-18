package com.skydevs.tgdrive.exception.user;

import com.skydevs.tgdrive.exception.BaseException;

public class PasswordErrorException extends BaseException {
    public PasswordErrorException(){
        super("密码错误");
    }

    public PasswordErrorException(String msg) {
        super(msg);
    }
}
