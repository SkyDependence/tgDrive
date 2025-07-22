package com.skydevs.tgdrive.exception;

public class PasswordErrorException extends BaseException{
    public PasswordErrorException(){
        super("密码错误");
    }

    public PasswordErrorException(String msg) {
        super(msg);
    }
}
