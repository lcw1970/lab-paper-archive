package com.lab.paperarchive.admin;

import java.time.LocalDateTime;
import java.util.List;

public record OperationalStatus(
        LocalDateTime checkedAt,
        LocalDateTime applicationStartedAt,
        boolean databaseConnected,
        String storageRoot,
        boolean storageWritable,
        long storageTotalBytes,
        long storageUsableBytes,
        long activePapers,
        long deletedPapers,
        long registeredFiles,
        long missingOriginalFiles,
        long orphanOriginalFiles,
        long readableCopies,
        List<String> warnings
) {
    public String storageTotal() {
        return formatBytes(storageTotalBytes);
    }

    public String storageUsable() {
        return formatBytes(storageUsableBytes);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "확인 불가";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return unit == 0 ? "%d %s".formatted(bytes, units[unit])
                : "%.1f %s".formatted(value, units[unit]);
    }
}
