package com.lab.paperarchive.log;

import com.lab.paperarchive.paper.PaperFile;
import com.lab.paperarchive.user.LabUserDetails;
import com.lab.paperarchive.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DownloadLogService {

    private final DownloadLogRepository downloadLogRepository;
    private final UserRepository userRepository;

    /** 로그 기록 실패가 다운로드를 막지 않도록 독립 트랜잭션으로 분리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LabUserDetails me, PaperFile file, HttpServletRequest request) {
        downloadLogRepository.save(DownloadLog.builder()
                .user(userRepository.findById(me.getId()).orElse(null))
                .userEmail(me.getEmail())
                .paperFile(file)
                .paperTitle(file.getPaper().getTitle())
                .ip(clientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .build());
    }

    /** 리버스 프록시(nginx) 뒤에서도 실제 IP를 얻는다. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
