package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PartResult {
    private final int index;
    private final byte[] data;
}
