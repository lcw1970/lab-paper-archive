package com.lab.paperarchive.user;

import com.lab.paperarchive.user.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupRequest form,
                         BindingResult bindingResult) {

        if (!form.isPasswordMatched()) {
            bindingResult.rejectValue("passwordConfirm", "mismatch",
                    "비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            signupService.signup(form);
        } catch (IllegalStateException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "auth/signup";
        }

        return "redirect:/signup/done";
    }

    @GetMapping("/signup/done")
    public String signupDone() {
        return "auth/pending";
    }
}
