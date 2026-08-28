package com.lab.paperarchive.user;

import com.lab.paperarchive.user.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 가입 신청 — 항상 PENDING 상태로 생성된다. 관리자 승인 후 로그인 가능. */
    @Transactional
    public Long signup(SignupRequest request) {
        String email = request.getEmail().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("이미 가입 신청된 이메일입니다.");
        }

        User user = User.signup(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getName().trim());

        return userRepository.save(user).getId();
    }
}
