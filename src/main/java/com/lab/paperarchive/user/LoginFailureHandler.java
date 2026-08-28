package com.lab.paperarchive.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String code = switch (exception) {
            case DisabledException e -> "pending";     // 승인 대기
            case LockedException e   -> "suspended";   // 정지 계정
            default                  -> "bad";         // 이메일/비밀번호 불일치
        };

        setDefaultFailureUrl("/login?error=" + code);
        super.onAuthenticationFailure(request, response, exception);
    }
}
