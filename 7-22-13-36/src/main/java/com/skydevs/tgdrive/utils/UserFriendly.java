package com.skydevs.tgdrive.utils;

public class UserFriendly {
    public static String humanReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = (int) (Math.log10(size) / Math.log10(1024));
        double readableSize = size / Math.pow(1024, unitIndex);
        return String.format("%.1f %s", readableSize, units[unitIndex]);
    }
}
