package com.lab.paperarchive.storage;

import com.lab.paperarchive.paper.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 이미 등록된 논문도 배포 직후 보기용 폴더에 생성한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadablePaperStorageInitializer {

    private final PaperRepository paperRepository;
    private final ReadablePaperStorage readablePaperStorage;

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeExistingPapers() {
        var papers = paperRepository.findAllActiveWithFilesAndFolder();
        papers.forEach(readablePaperStorage::synchronize);
        log.info("보기용 논문 사본 동기화 완료: {}편", papers.size());
    }
}
