package com.skydevs.tgdrive.entity;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BigFileInfo {
    private String fileName;
    private long fileSize;
    private List<String> fileIds;
    private boolean isRecordFile;
}
