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
public class ChangePasswordRequest {
    @NotBlank(message = "旧密码不能位空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    @Length(min = 5, message = "密码至少为5位")
    private String newPassword;
}
