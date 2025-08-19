package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckChunkResponse {
    private boolean skipUpload;
    private Set<Integer> uploadedChunks;
}
