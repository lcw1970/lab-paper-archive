package com.lab.paperarchive.storage;

import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorage implements StorageService {

    private static final String TMP_DIR = "tmp";
    private static final String TRASH_DIR = "trash";

    private final Path root;
    private final PdfValidator pdfValidator;
    private final long maxFileSize;

    public LocalFileStorage(AppProperties properties, PdfValidator pdfValidator) {
        this.root = Paths.get(properties.storage().root()).toAbsolutePath().normalize();
        this.pdfValidator = pdfValidator;
        this.maxFileSize = properties.storage().maxFileSizeBytes();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root.resolve(TMP_DIR));
            Files.createDirectories(root.resolve(TRASH_DIR));
            log.info("파일 저장소 루트: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("저장소 초기화 실패: " + root, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        pdfValidator.validate(file);
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("파일 크기가 허용 범위를 초과했습니다.");
        }

        Path tmp = root.resolve(TMP_DIR).resolve(UUID.randomUUID().toString());
        String sha256;

        // 스트리밍 저장과 해시 계산을 한 번에 처리한다 (대용량 PDF 메모리 절약)
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                Files.copy(dis, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            sha256 = HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new BusinessException("파일 저장에 실패했습니다.");
        }

        YearMonth now = YearMonth.now();
        String relativePath = "%d/%02d/%s.pdf".formatted(
                now.getYear(), now.getMonthValue(), UUID.randomUUID());

        try {
            Path dest = resolveSafely(relativePath);
            Files.createDirectories(dest.getParent());
            Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new BusinessException("파일 이동에 실패했습니다.");
        }

        return new StoredFile(relativePath, sha256, file.getSize());
    }

    @Override
    public Resource load(String relativePath) {
        Path path = resolveSafely(relativePath);
        if (!Files.exists(path)) {
            throw new BusinessException("파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void moveToTrash(String relativePath) {
        Path src = resolveSafely(relativePath);
        if (!Files.exists(src)) return;

        Path dest = root.resolve(TRASH_DIR).resolve(relativePath.replace('/', '_'));
        try {
            Files.createDirectories(dest.getParent());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("휴지통 이동 실패: {}", relativePath, e);
        }
    }

    @Override
    public void deletePermanently(String relativePath) {
        Path path = resolveSafely(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("저장 파일 영구 삭제 실패: {}", relativePath, e);
            throw new BusinessException("PDF 파일을 영구 삭제하지 못했습니다.");
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolveSafely(relativePath));
    }

    /** Path Traversal 방어 — 결과 경로가 반드시 root 하위여야 한다. */
    private Path resolveSafely(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("허용되지 않은 경로: " + relativePath);
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
