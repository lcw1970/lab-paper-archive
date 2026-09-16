package com.lab.paperarchive.admin;

import com.lab.paperarchive.config.AppProperties;
import com.lab.paperarchive.paper.PaperFileRepository;
import com.lab.paperarchive.paper.PaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OperationalStatusService {

    private final PaperRepository paperRepository;
    private final PaperFileRepository paperFileRepository;
    private final AppProperties appProperties;
    private final LocalDateTime applicationStartedAt = LocalDateTime.now();

    public OperationalStatus inspect() {
        List<String> warnings = new ArrayList<>();
        Path root = Path.of(appProperties.storage().root()).toAbsolutePath().normalize();

        boolean databaseConnected = true;
        long activePapers = -1;
        long deletedPapers = -1;
        long registeredFiles = -1;
        long missingOriginalFiles = -1;
        long orphanOriginalFiles = -1;
        List<String> storedPaths = List.of();

        try {
            activePapers = paperRepository.countByDeletedAtIsNull();
            deletedPapers = paperRepository.countByDeletedAtIsNotNull();
            storedPaths = paperFileRepository.findAllStoredPaths();
            registeredFiles = storedPaths.size();
        } catch (RuntimeException e) {
            databaseConnected = false;
            activePapers = -1;
            deletedPapers = -1;
            registeredFiles = -1;
            storedPaths = List.of();
            warnings.add("데이터베이스 상태를 확인하지 못했습니다.");
        }

        if (databaseConnected) {
            Set<Path> registeredPaths = new HashSet<>();
            missingOriginalFiles = countMissingOriginals(root, storedPaths, registeredPaths);
            orphanOriginalFiles = countOrphanOriginals(root, registeredPaths, warnings);
            if (missingOriginalFiles > 0) {
                warnings.add("DB에는 있지만 디스크에서 찾지 못한 원본 PDF가 있습니다.");
            }
            if (orphanOriginalFiles > 0) {
                warnings.add("DB에 연결되지 않은 원본 PDF가 있습니다. 자동 삭제하지 않습니다.");
            }
        }

        long readableCopies = countRegularFiles(root.resolve("readable"), warnings);
        long totalBytes = -1;
        long usableBytes = -1;
        try {
            FileStore fileStore = Files.getFileStore(root);
            totalBytes = fileStore.getTotalSpace();
            usableBytes = fileStore.getUsableSpace();
        } catch (IOException e) {
            warnings.add("저장공간 용량을 확인하지 못했습니다.");
        }

        boolean storageWritable = Files.isWritable(root);
        if (!storageWritable) {
            warnings.add("논문 저장 폴더에 쓰기 권한이 없습니다.");
        }

        return new OperationalStatus(
                LocalDateTime.now(), applicationStartedAt, databaseConnected,
                root.toString(), storageWritable, totalBytes, usableBytes,
                activePapers, deletedPapers, registeredFiles, missingOriginalFiles,
                orphanOriginalFiles, readableCopies, List.copyOf(warnings)
        );
    }

    private long countMissingOriginals(Path root, List<String> storedPaths, Set<Path> registeredPaths) {
        long missing = 0;
        for (String storedPath : storedPaths) {
            Path resolved = root.resolve(storedPath).normalize();
            if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                missing++;
            } else {
                registeredPaths.add(resolved);
            }
        }
        return missing;
    }

    private long countOrphanOriginals(Path root, Set<Path> registeredPaths, List<String> warnings) {
        if (!Files.isDirectory(root)) return 0;
        Path readable = root.resolve("readable");
        Path trash = root.resolve("trash");
        Path tmp = root.resolve("tmp");
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .filter(path -> !path.startsWith(readable))
                    .filter(path -> !path.startsWith(trash))
                    .filter(path -> !path.startsWith(tmp))
                    .filter(path -> !registeredPaths.contains(path.normalize()))
                    .count();
        } catch (IOException e) {
            warnings.add("DB에 연결되지 않은 PDF 검사를 완료하지 못했습니다.");
            return -1;
        }
    }

    private long countRegularFiles(Path directory, List<String> warnings) {
        if (!Files.isDirectory(directory)) return 0;
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            warnings.add("보기용 PDF 개수를 확인하지 못했습니다.");
            return -1;
        }
    }
}
