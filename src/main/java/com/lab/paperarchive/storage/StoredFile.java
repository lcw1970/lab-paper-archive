package com.lab.paperarchive.storage;

/** 저장 완료된 파일 정보 */
public record StoredFile(
        String relativePath,   // app.storage.root 기준 상대경로
        String sha256,
        long size
) {}
