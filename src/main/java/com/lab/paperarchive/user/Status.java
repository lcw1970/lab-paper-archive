package com.lab.paperarchive.user;

public enum Status {
    PENDING,    // 가입 신청 — 관리자 승인 대기
    ACTIVE,     // 정상 이용
    SUSPENDED   // 정지 (졸업생 등) — 이력 보존을 위해 삭제하지 않음
}
