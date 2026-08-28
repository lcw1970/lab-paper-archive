package com.lab.paperarchive.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    public boolean isPasswordMatched() {
        return password != null && password.equals(passwordConfirm);
    }
}
