package com.lab.paperarchive.paper.dto;

/** 폴더 탐색 화면에 표시할 폴더명과 활성 논문 수. */
public record FolderSummary(Long id, String name, long paperCount) {
}
