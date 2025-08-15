package com.skydevs.tgdrive.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Setting {
    private int id;
    @NotBlank(message = "key不能为空")
    private String key;
    @NotBlank(message = "value不能为空")
    private String value;
    private String description;
}
