package com.lab.paperarchive.admin;

import com.lab.paperarchive.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @PageableDefault(size = 50) Pageable pageable,
                       Model model) {
        model.addAttribute("logs", auditLogService.search(q, pageable));
        model.addAttribute("q", q);
        return "admin/audit";
    }
}
