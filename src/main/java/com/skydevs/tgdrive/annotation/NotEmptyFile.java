package com.skydevs.tgdrive.annotation;

import com.skydevs.tgdrive.annotation.impl.NotEmptyFileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyFileValidator.class)
public @interface NotEmptyFile {
    String message() default "上传的文件不能为空";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
