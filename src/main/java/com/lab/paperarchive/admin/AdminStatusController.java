package com.lab.paperarchive.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/status")
@RequiredArgsConstructor
public class AdminStatusController {

    private final OperationalStatusService operationalStatusService;

    @GetMapping
    public String status(Model model) {
        model.addAttribute("status", operationalStatusService.inspect());
        return "admin/status";
    }
}
