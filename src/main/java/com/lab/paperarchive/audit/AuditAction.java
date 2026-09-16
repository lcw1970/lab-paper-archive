package com.lab.paperarchive.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditAction {
    PAPER_UPLOAD("논문 등록"),
    PAPER_UPDATE("논문 정보 수정"),
    PAPER_MOVE("논문 폴더 이동"),
    PAPER_DELETE("휴지통 이동"),
    PAPER_RESTORE("논문 복원"),
    PAPER_PERMANENT_DELETE("논문 영구 삭제"),
    FOLDER_CREATE("폴더 생성"),
    FOLDER_DELETE("폴더 삭제"),
    USER_APPROVE("회원 승인"),
    USER_SUSPEND("회원 정지"),
    USER_ACTIVATE("회원 재개");

    private final String label;
}
