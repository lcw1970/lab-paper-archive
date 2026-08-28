package com.lab.paperarchive.paper;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "paper_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaperFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    /** app.storage.root 기준 상대경로. 절대경로 저장 금지 */
    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType = "application/pdf";

    /** 파일 버전. JPA 낙관적 락(@Version)이 아님에 주의 */
    @Column(nullable = false)
    private int version = 1;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Builder
    private PaperFile(String storedPath, String originalName, long fileSize,
                      String sha256, String contentType, int version) {
        this.storedPath = storedPath;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.contentType = contentType != null ? contentType : "application/pdf";
        this.version = version > 0 ? version : 1;
        this.uploadedAt = LocalDateTime.now();
    }

    void assignTo(Paper paper) {
        this.paper = paper;
    }
}
