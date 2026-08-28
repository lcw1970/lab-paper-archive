package com.lab.paperarchive.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadLogRepository extends JpaRepository<DownloadLog, Long> {

    Page<DownloadLog> findAllByOrderByDownloadedAtDesc(Pageable pageable);

    Page<DownloadLog> findByUserIdOrderByDownloadedAtDesc(Long userId, Pageable pageable);
}
