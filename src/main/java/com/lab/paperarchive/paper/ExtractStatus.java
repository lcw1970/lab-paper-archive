package com.lab.paperarchive.paper;

public enum ExtractStatus {
    PENDING,   // 추출 대기
    DONE,      // 추출 완료
    FAILED,    // 추출 실패 (스캔본 등)
    SKIPPED    // 추출 생략 (페이지 초과 등)
}
