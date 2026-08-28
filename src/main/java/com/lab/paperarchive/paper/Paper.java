package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.BaseTimeEntity;
import com.lab.paperarchive.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "papers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paper extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String authors;

    @Column(length = 300)
    private String venue;

    @Column(name = "pub_year")
    private Integer pubYear;

    @Column(length = 200)
    private String doi;

    @Column(name = "abstract_text", columnDefinition = "text")
    private String abstractText;

    @Column(columnDefinition = "text")
    private String memo;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "extract_status", nullable = false, length = 20)
    private ExtractStatus extractStatus = ExtractStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version DESC")
    private List<PaperFile> files = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_tags",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    @Builder
    private Paper(String title, String authors, String venue, Integer pubYear,
                  String doi, String abstractText, String memo, User uploader) {
        this.title = title;
        this.authors = authors;
        this.venue = venue;
        this.pubYear = pubYear;
        this.doi = doi;
        this.abstractText = abstractText;
        this.memo = memo;
        this.uploader = uploader;
        this.extractStatus = ExtractStatus.PENDING;
    }

    // ── 연관관계 편의 메서드 ──
    public void addFile(PaperFile file) {
        this.files.add(file);
        file.assignTo(this);
    }

    public void addTag(Tag tag)    { this.tags.add(tag); }
    public void clearTags()        { this.tags.clear(); }

    /** 다음 버전 번호 — 수정본 업로드 시 사용 */
    public int nextVersion() {
        return files.stream()
                .mapToInt(PaperFile::getVersion)
                .max().orElse(0) + 1;
    }

    public void updateMetadata(String title, String authors, String venue,
                               Integer pubYear, String doi,
                               String abstractText, String memo) {
        this.title = title;
        this.authors = authors;
        this.venue = venue;
        this.pubYear = pubYear;
        this.doi = doi;
        this.abstractText = abstractText;
        this.memo = memo;
    }

    public void applyExtractedText(String text) {
        this.extractedText = text;
        this.extractStatus = ExtractStatus.DONE;
    }

    public void markExtractStatus(ExtractStatus status) {
        this.extractStatus = status;
    }

    /** soft delete — 실제 파일은 배치로 지운다 */
    public void softDelete()  { this.deletedAt = LocalDateTime.now(); }
    public void restore()     { this.deletedAt = null; }
    public boolean isDeleted() { return this.deletedAt != null; }
}
