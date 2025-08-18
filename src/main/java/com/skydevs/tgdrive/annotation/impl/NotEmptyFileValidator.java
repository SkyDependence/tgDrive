package com.skydevs.tgdrive.annotation.impl;

import com.skydevs.tgdrive.annotation.NotEmptyFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class NotEmptyFileValidator implements ConstraintValidator<NotEmptyFile, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile multipartFile, ConstraintValidatorContext constraintValidatorContext) {
        return multipartFile != null && !multipartFile.isEmpty();
    }
}
