package com.lab.paperarchive.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 추상화.
 * 향후 NAS·S3로 이전하더라도 이 인터페이스만 지키면 나머지 코드는 변경되지 않는다.
 */
public interface StorageService {

    StoredFile store(MultipartFile file);

    Resource load(String relativePath);

    void moveToTrash(String relativePath);

    boolean exists(String relativePath);
}
