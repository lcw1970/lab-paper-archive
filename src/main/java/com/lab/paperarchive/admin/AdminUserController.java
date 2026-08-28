package com.lab.paperarchive.admin;

import com.lab.paperarchive.user.LabUserDetails;
import com.lab.paperarchive.user.Status;
import com.lab.paperarchive.user.User;
import com.lab.paperarchive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pendingUsers",
                userRepository.findByStatusOrderByCreatedAtAsc(Status.PENDING));
        model.addAttribute("allUsers",
                userRepository.findAllByOrderByStatusAscCreatedAtDesc());
        return "admin/users";
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(User::approve);
        ra.addFlashAttribute("message", "승인 처리했습니다.");
        return "redirect:/admin/users";
    }

    /** 졸업생 등 — 계정은 남기고 접근만 차단 */
    @PostMapping("/{id}/suspend")
    @Transactional
    public String suspend(@PathVariable Long id,
                          @AuthenticationPrincipal LabUserDetails me,
                          RedirectAttributes ra) {
        if (me.getId().equals(id)) {
            ra.addFlashAttribute("error", "자기 자신은 정지할 수 없습니다.");
            return "redirect:/admin/users";
        }
        userRepository.findById(id).ifPresent(User::suspend);
        ra.addFlashAttribute("message", "정지 처리했습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    @Transactional
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(User::approve);
        ra.addFlashAttribute("message", "이용 재개 처리했습니다.");
        return "redirect:/admin/users";
    }
}
