package com.lab.paperarchive.user;

import com.lab.paperarchive.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;          // BCrypt 해시

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private User(String email, String password, String name, Role role, Status status) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role != null ? role : Role.MEMBER;
        this.status = status != null ? status : Status.PENDING;
    }

    /** 일반 가입 — 항상 승인 대기 상태로 생성된다. */
    public static User signup(String email, String encodedPassword, String name) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.MEMBER)
                .status(Status.PENDING)
                .build();
    }
    /** 최초 관리자 계정 — 초기화 시에만 사용. 즉시 ACTIVE. */
    public static User createAdmin(String email, String encodedPassword, String name) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();
    }

    public void approve()   { this.status = Status.ACTIVE; }
    public void suspend()   { this.status = Status.SUSPENDED; }
    public void grantAdmin() { this.role = Role.ADMIN; }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }
}
