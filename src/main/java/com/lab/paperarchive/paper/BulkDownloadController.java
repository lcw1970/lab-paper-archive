package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.log.DownloadLogService;
import com.lab.paperarchive.storage.StorageService;
import com.lab.paperarchive.user.LabUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BulkDownloadController {

    private static final int MAX_BULK = 50;

    private final PaperRepository paperRepository;
    private final StorageService storageService;
    private final DownloadLogService downloadLogService;

    @PostMapping("/papers/bulk-download")
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> bulkDownload(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @AuthenticationPrincipal LabUserDetails principal,
            HttpServletRequest request) {

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("선택된 논문이 없습니다.");
        }
        if (ids.size() > MAX_BULK) {
            throw new BusinessException("한 번에 최대 " + MAX_BULK + "편까지 받을 수 있습니다.");
        }

        List<Paper> papers = paperRepository.findAllForDownload(ids);
        if (papers.isEmpty()) {
            throw new BusinessException("다운로드할 논문이 없습니다.");
        }

        // 각 논문의 최신 버전 파일만 뽑는다
        List<PaperFile> targets = new ArrayList<>();
        for (Paper p : papers) {
            p.getFiles().stream()
                    .max(Comparator.comparingInt(PaperFile::getVersion))
                    .ifPresent(targets::add);
        }
        if (targets.isEmpty()) {
            throw new BusinessException("다운로드할 파일이 없습니다.");
        }

        // 스트리밍 시작 전에 로그부터 남긴다.
        // 전송이 중간에 끊겨도 "요청했다"는 사실은 기록되어야 한다.
        for (PaperFile f : targets) {
            downloadLogService.record(principal, f, request);
        }
        log.info("일괄 다운로드: user={}, 편수={}", principal.getUsername(), targets.size());

        // ZIP 엔트리 정보를 미리 뽑아둔다 (스트리밍 시점엔 세션이 닫혀 있다)
        List<String[]> entries = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for (PaperFile f : targets) {
            entries.add(new String[]{
                    uniqueName(usedNames, f.getOriginalName()),
                    f.getStoredPath()
            });
        }

        String zipName = "papers_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + "_" + entries.size() + ".zip";

        StreamingResponseBody body = out -> writeZip(out, entries);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''"
                                + URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    private void writeZip(OutputStream out, List<String[]> entries) {
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.setLevel(0);   // PDF는 이미 압축되어 있다. 재압축은 CPU만 먹는다.
            for (String[] e : entries) {
                Resource resource = storageService.load(e[1]);
                if (!resource.exists()) {
                    log.warn("ZIP 대상 파일 없음: {}", e[1]);
                    continue;
                }
                zip.putNextEntry(new ZipEntry(e[0]));
                try (InputStream in = resource.getInputStream()) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (Exception ex) {
            // 브라우저가 취소하면 여기로 온다. 정상 상황이므로 스택트레이스는 남기지 않는다.
            log.warn("ZIP 스트리밍 중단: {}", ex.toString());
        }
    }

    /** ZIP 안에서 파일명이 겹치지 않도록 (2), (3)을 붙인다 */
    private String uniqueName(Set<String> used, String original) {
        String base = (original == null || original.isBlank()) ? "unknown.pdf" : original;
        base = base.replaceAll("[/\\\\]", "_");
        if (used.add(base)) return base;

        String stem = base.replaceFirst("(?i)\\.pdf$", "");
        for (int i = 2; ; i++) {
            String candidate = stem + " (" + i + ").pdf";
            if (used.add(candidate)) return candidate;
        }
    }
}
