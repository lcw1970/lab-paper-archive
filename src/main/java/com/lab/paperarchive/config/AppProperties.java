package com.lab.paperarchive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Storage storage,
        Admin admin,
        Extract extract
) {
    public record Storage(String root, long maxFileSizeBytes, int trashRetentionDays) {}
    public record Admin(String email, String password) {}
    public record Extract(boolean enabled, int maxPages) {}
}