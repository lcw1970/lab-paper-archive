package com.lab.paperarchive.config;

import com.lab.paperarchive.user.User;
import com.lab.paperarchive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 최초 1회 관리자 계정을 생성한다. 이미 있으면 아무것도 하지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.admin().email();
        String rawPassword = properties.admin().password();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(rawPassword)) {
            log.warn("app.admin.email / app.admin.password 미설정 — 관리자 계정 생성을 건너뜁니다.");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        userRepository.save(
                User.createAdmin(email, passwordEncoder.encode(rawPassword), "관리자"));
        log.info("최초 관리자 계정을 생성했습니다: {}", email);
    }
}
