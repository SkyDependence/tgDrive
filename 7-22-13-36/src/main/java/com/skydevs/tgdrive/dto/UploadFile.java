package com.skydevs.tgdrive.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UploadFile {
    private String fileName;
    private String downloadLink;
}
