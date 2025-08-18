package com.skydevs.tgdrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigForm {
    @NotBlank(message = "配置名不能为空")
    private String name;
    @NotBlank(message = "token不能为空")
    private String token;
    @NotBlank(message = "target不能为空")
    private String target;
    private String pass;
    private String url;
}
