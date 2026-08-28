package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.log.DownloadLogService;
import com.lab.paperarchive.storage.StorageService;
import com.lab.paperarchive.user.LabUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class FileDownloadController {

    private final PaperFileRepository paperFileRepository;
    private final StorageService storageService;
    private final DownloadLogService downloadLogService;

    @GetMapping("/files/{fileId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal LabUserDetails me,
            HttpServletRequest request) {

        PaperFile paperFile = paperFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        if (paperFile.getPaper().isDeleted()) {
            throw new BusinessException("삭제된 논문입니다.");
        }

        // 저작권 대응 — 모든 접근을 기록한다
        downloadLogService.record(me, paperFile, request);

        Resource resource = storageService.load(paperFile.getStoredPath());
        String encodedName = UriUtils.encode(paperFile.getOriginalName(), StandardCharsets.UTF_8);
        String disposition = (inline ? "inline" : "attachment")
                + "; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }
}
