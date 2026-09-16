package com.lab.paperarchive.admin;

import com.lab.paperarchive.user.LabUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
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

    private final AdminUserService adminUserService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pendingUsers", adminUserService.findPendingUsers());
        model.addAttribute("allUsers", adminUserService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        adminUserService.approve(id);
        ra.addFlashAttribute("message", "승인 처리했습니다.");
        return "redirect:/admin/users";
    }

    /** 졸업생 등 — 계정은 남기고 접근만 차단 */
    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable Long id,
                          @AuthenticationPrincipal LabUserDetails me,
                          RedirectAttributes ra) {
        adminUserService.suspend(id, me.getId());
        ra.addFlashAttribute("message", "정지 처리했습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        adminUserService.activate(id);
        ra.addFlashAttribute("message", "이용 재개 처리했습니다.");
        return "redirect:/admin/users";
    }
}
