package com.lab.paperarchive.paper;

import com.lab.paperarchive.paper.dto.PaperUploadRequest;
import com.lab.paperarchive.user.LabUserDetails;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final PaperQueryService paperQueryService;
    private final TagRepository tagRepository;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String tag,
                       @PageableDefault(size = 20, sort = "createdAt",
                               direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("papers", paperQueryService.search(q, tag, pageable));
        model.addAttribute("q", q);
        model.addAttribute("tag", tag);
        model.addAttribute("tags", tagRepository.findAll());
        return "paper/list";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new PaperUploadRequest());
        }
        return "paper/upload";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute("form") PaperUploadRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LabUserDetails principal,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return "paper/upload";
        }
        Long id = paperService.upload(form, principal.getUserId());
        ra.addFlashAttribute("message", "등록되었습니다.");
        return "redirect:/papers/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("paper", paperQueryService.findDetail(id));
        return "paper/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        paperService.softDelete(id);
        ra.addFlashAttribute("message", "휴지통으로 이동했습니다.");
        return "redirect:/papers";
    }
}
