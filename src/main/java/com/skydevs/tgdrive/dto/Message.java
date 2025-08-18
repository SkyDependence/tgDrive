package com.skydevs.tgdrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @NotBlank(message = "消息不能为空")
    private String message;
}
