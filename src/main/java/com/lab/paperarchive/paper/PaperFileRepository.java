package com.lab.paperarchive.paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaperFileRepository extends JpaRepository<PaperFile, Long> {

    /** 삭제되지 않은 논문만 중복 업로드로 판단한다. */
    Optional<PaperFile> findBySha256AndPaper_DeletedAtIsNull(String sha256);

    List<PaperFile> findByPaperIdOrderByVersionDesc(Long paperId);

    @Query("select pf.storedPath from PaperFile pf")
    List<String> findAllStoredPaths();
}
