package com.lab.paperarchive.audit;

import com.lab.paperarchive.user.LabUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(AuditAction action, String targetType, Long targetId, String description) {
        Actor actor = currentActor();
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actor.userId())
                .actorEmail(actor.email())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findRecent(Pageable pageable) {
        return auditLogRepository.findAllByOrderByOccurredAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String query, Pageable pageable) {
        String keyword = StringUtils.hasText(query) ? query.trim() : "";
        return auditLogRepository.search(
                Normalizer.normalize(keyword, Normalizer.Form.NFC),
                Normalizer.normalize(keyword, Normalizer.Form.NFD),
                pageable
        );
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LabUserDetails principal) {
            return new Actor(principal.getId(), principal.getEmail());
        }
        return new Actor(null, "SYSTEM");
    }

    private record Actor(Long userId, String email) {
    }
}
