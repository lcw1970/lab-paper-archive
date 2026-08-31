package com.lab.paperarchive.storage;

import com.lab.paperarchive.config.AppProperties;
import com.lab.paperarchive.paper.Paper;
import com.lab.paperarchive.paper.PaperFile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 앱이 관리하는 UUID 원본과 별개로, 사람이 탐색기에서 보기 쉬운 PDF 사본을 만든다.
 * 보기용 파일은 언제든 원본에서 다시 생성할 수 있으므로 직접 수정하지 않는 것을 원칙으로 한다.
 */
@Slf4j
@Service
public class ReadablePaperStorage {

    private static final String READABLE_DIR = "readable";

    private final Path root;
    private Path readableRoot;

    public ReadablePaperStorage(AppProperties properties) {
        this.root = Path.of(properties.storage().root()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        readableRoot = root.resolve(READABLE_DIR);
        try {
            Files.createDirectories(readableRoot);
        } catch (IOException e) {
            throw new IllegalStateException("보기용 논문 폴더를 만들 수 없습니다: " + readableRoot, e);
        }
    }

    /** 현재 논문 정보(폴더, 제목)에 맞춰 보기용 PDF 사본을 다시 만든다. */
    public void synchronize(Paper paper) {
        if (paper.getId() == null) {
            log.warn("보기용 사본 생성 생략: 저장되지 않은 논문입니다.");
            return;
        }

        try {
            removeCopies(paper.getId());
            if (paper.isDeleted()) return;

            Path folder = readableRoot.resolve(safeName(
                    paper.getFolder() == null ? "미분류" : paper.getFolder().getName(), 80));
            Files.createDirectories(folder);

            for (PaperFile file : paper.getFiles()) {
                Path source = resolveSource(file.getStoredPath());
                if (!Files.isRegularFile(source)) {
                    log.warn("보기용 사본 생성 생략: 원본 PDF를 찾을 수 없습니다. paperId={}, path={}",
                            paper.getId(), file.getStoredPath());
                    continue;
                }

                Path destination = folder.resolve(readableFileName(paper, file));
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // 보기용 폴더는 원본 데이터가 아니라 편의 기능이므로, 논문 등록·수정을 실패시키지 않는다.
            log.error("보기용 논문 사본 동기화 실패: paperId={}", paper.getId(), e);
        }
    }

    /** 휴지통 이동 또는 영구 삭제 시 보기용 사본을 제거한다. */
    public void removeCopies(Long paperId) {
        if (paperId == null || !Files.isDirectory(readableRoot)) return;

        String marker = "__paper-" + paperId + "__";
        try (Stream<Path> paths = Files.walk(readableRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(marker))
                    .forEach(this::deleteQuietly);
        } catch (IOException e) {
            log.error("보기용 논문 사본 삭제 실패: paperId={}", paperId, e);
        }
        removeEmptyDirectories();
    }

    private Path resolveSource(String relativePath) {
        Path source = root.resolve(relativePath).normalize();
        if (!source.startsWith(root) || source.startsWith(readableRoot)) {
            throw new SecurityException("허용되지 않은 원본 경로입니다.");
        }
        return source;
    }

    private String readableFileName(Paper paper, PaperFile file) {
        return "%s__paper-%d__v%d.pdf".formatted(
                safeName(paper.getTitle(), 120), paper.getId(), file.getVersion());
    }

    /** Windows와 Linux 양쪽에서 안전한 파일·폴더명으로 바꾼다. */
    private String safeName(String raw, int maxLength) {
        String cleaned = raw == null ? "" : raw
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[. ]+$", "");
        if (cleaned.isBlank()) cleaned = "이름 없음";
        if (isWindowsReservedName(cleaned)) cleaned = "_" + cleaned;
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength).trim() : cleaned;
    }

    private boolean isWindowsReservedName(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("보기용 논문 사본 삭제 실패: {}", path, e);
        }
    }

    private void removeEmptyDirectories() {
        try (Stream<Path> paths = Files.walk(readableRoot)) {
            paths.filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(readableRoot))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                            // 비어 있지 않은 폴더는 유지한다.
                        }
                    });
        } catch (IOException e) {
            log.warn("비어 있는 보기용 폴더 정리 실패", e);
        }
    }
}
