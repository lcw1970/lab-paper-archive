package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.paper.dto.PaperUploadRequest;
import com.lab.paperarchive.storage.StorageService;
import com.lab.paperarchive.storage.StoredFile;
import com.lab.paperarchive.user.User;
import com.lab.paperarchive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperRepository paperRepository;
    private final PaperFileRepository paperFileRepository;
    private final TagRepository tagRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public Long upload(PaperUploadRequest request, Long uploaderId) {
        // 1) 디스크에 저장하고 해시를 얻는다
        StoredFile stored = storageService.store(request.getFile());

        // 2) 중복 검사 — DB UNIQUE 제약과 이중 방어
        paperFileRepository.findBySha256AndPaper_DeletedAtIsNull(stored.sha256()).ifPresent(existing -> {
            storageService.moveToTrash(stored.relativePath());
            throw new BusinessException(
                    "이미 등록된 논문입니다: " + existing.getPaper().getTitle());
        });

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Paper paper = Paper.builder()
                .title(request.getTitle().trim())
                .authors(trimOrNull(request.getAuthors()))
                .memo(trimOrNull(request.getMemo()))
                .uploader(uploader)
                .build();

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new BusinessException("선택한 폴더를 찾을 수 없습니다."));
            paper.assignFolder(folder);
        }


        paper.addFile(PaperFile.builder()
                .storedPath(stored.relativePath())
                .originalName(sanitizeFileName(request.getFile().getOriginalFilename()))
                .fileSize(stored.size())
                .sha256(stored.sha256())
                .contentType("application/pdf")
                .version(1)
                .build());

        attachTags(paper, request.getTags());

        Long id = paperRepository.save(paper).getId();
        log.info("논문 등록: id={}, title={}, uploader={}", id, paper.getTitle(), uploader.getEmail());
        return id;
    }

    @Transactional
    public void softDelete(Long paperId) {
        Paper paper = paperRepository.findByIdAndDeletedAtIsNull(paperId)
                .orElseThrow(() -> new BusinessException("논문을 찾을 수 없습니다."));
        paper.softDelete();
        // 실제 파일은 즉시 지우지 않는다. 휴지통 배치가 30일 후 정리한다.
    }

    @Transactional
    public int softDeleteAll(Iterable<Long> ids) {
        java.util.List<Long> requestedIds = new java.util.ArrayList<>();
        ids.forEach(requestedIds::add);
        if (requestedIds.isEmpty()) {
            throw new BusinessException("선택된 논문이 없습니다.");
        }

        java.util.List<Paper> papers = paperRepository.findAllByIdInAndDeletedAtIsNull(requestedIds);
        if (papers.isEmpty()) {
            throw new BusinessException("삭제할 논문을 찾을 수 없습니다.");
        }
        papers.forEach(Paper::softDelete);
        return papers.size();
    }

    private void attachTags(Paper paper, String rawTags) {
        if (!StringUtils.hasText(rawTags)) return;

        Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(10)
                .forEach(name -> {
                    Tag tag = tagRepository.findByNameIgnoreCase(name)
                            .orElseGet(() -> tagRepository.save(Tag.of(name)));
                    paper.addTag(tag);
                });
    }

    private String trimOrNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    /** 원본 파일명에서 경로 구분자를 제거한다. */
    private String sanitizeFileName(String name) {
        if (!StringUtils.hasText(name)) return "unknown.pdf";
        String cleaned = name.replaceAll("[/\\\\]", "_");
        return cleaned.length() > 255 ? cleaned.substring(cleaned.length() - 255) : cleaned;
    }
}
