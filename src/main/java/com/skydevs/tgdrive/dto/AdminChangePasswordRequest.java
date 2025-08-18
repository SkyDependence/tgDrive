package com.skydevs.tgdrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AdminChangePasswordRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "新密码不能为空")
    @Length(min = 5, message = "新密码长度不能少于5位")
    private String newPassword;
}