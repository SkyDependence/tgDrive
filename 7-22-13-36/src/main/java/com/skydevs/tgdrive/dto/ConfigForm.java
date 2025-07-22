package com.skydevs.tgdrive.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigForm {
    private String name;
    private String token;
    private String target;
    private String pass;
    private String url;
}
