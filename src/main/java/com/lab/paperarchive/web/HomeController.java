package com.lab.paperarchive.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/papers";
    }


    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
