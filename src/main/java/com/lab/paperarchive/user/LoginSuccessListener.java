package com.lab.paperarchive.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 성공 시 마지막 접속 시각을 기록한다. */
@Component
@RequiredArgsConstructor
public class LoginSuccessListener {

    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof LabUserDetails details) {
            userRepository.findById(details.getId()).ifPresent(User::recordLogin);
        }
    }
}
