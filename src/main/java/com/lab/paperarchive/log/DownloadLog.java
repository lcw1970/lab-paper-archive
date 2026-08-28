package com.lab.paperarchive.log;

import com.lab.paperarchive.paper.PaperFile;
import com.lab.paperarchive.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 저작권 대응 감사 로그.
 * 사용자·논문이 삭제되어도 "누가 무엇을 받았는지" 읽을 수 있도록
 * 이메일과 논문 제목을 문자열로 복사 저장한다.
 */
@Entity
@Table(name = "download_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "action", nullable = false, length = 10)
    private String action = "DOWNLOAD";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_file_id")
    private PaperFile paperFile;

    @Column(name = "paper_title", nullable = false, length = 500)
    private String paperTitle;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "downloaded_at", nullable = false, updatable = false)
    private LocalDateTime downloadedAt = LocalDateTime.now();

    @Builder
    private DownloadLog(User user, String userEmail, PaperFile paperFile,
                        String paperTitle, String ip, String userAgent) {
        this.user = user;
        this.userEmail = userEmail;
        this.paperFile = paperFile;
        this.paperTitle = paperTitle;
        this.ip = ip;
        this.userAgent = truncate(userAgent, 300);
        this.downloadedAt = LocalDateTime.now();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
