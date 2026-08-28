package com.lab.paperarchive.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperFileRepository extends JpaRepository<PaperFile, Long> {

    Optional<PaperFile> findBySha256(String sha256);

    List<PaperFile> findByPaperIdOrderByVersionDesc(Long paperId);
}
