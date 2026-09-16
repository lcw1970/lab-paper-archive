package com.lab.paperarchive.admin;

import com.lab.paperarchive.audit.AuditAction;
import com.lab.paperarchive.audit.AuditLogService;
import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.user.Status;
import com.lab.paperarchive.user.User;
import com.lab.paperarchive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<User> findPendingUsers() {
        return userRepository.findByStatusOrderByCreatedAtAsc(Status.PENDING);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAllByOrderByStatusAscCreatedAtDesc();
    }

    @Transactional
    public void approve(Long userId) {
        User user = findUser(userId);
        user.approve();
        auditLogService.record(AuditAction.USER_APPROVE, "USER", userId, user.getEmail());
    }

    @Transactional
    public void suspend(Long userId, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException("자기 자신은 정지할 수 없습니다.");
        }
        User user = findUser(userId);
        user.suspend();
        auditLogService.record(AuditAction.USER_SUSPEND, "USER", userId, user.getEmail());
    }

    @Transactional
    public void activate(Long userId) {
        User user = findUser(userId);
        user.approve();
        auditLogService.record(AuditAction.USER_ACTIVATE, "USER", userId, user.getEmail());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));
    }
}
