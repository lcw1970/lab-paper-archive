package com.lab.paperarchive.paper.dto;

import com.lab.paperarchive.paper.Paper;

import java.time.LocalDateTime;

public record PaperListItem(
        Long id,
        String title,
        String authors,
        String uploaderName,
        LocalDateTime createdAt
) {
    public static PaperListItem from(Paper p) {
        return new PaperListItem(
                p.getId(),
                p.getTitle(),
                p.getAuthors(),
                p.getUploader() != null ? p.getUploader().getName() : "-",
                p.getCreatedAt()
        );
    }
}