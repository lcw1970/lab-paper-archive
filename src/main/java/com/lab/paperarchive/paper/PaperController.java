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
import org.springframework.util.StringUtils;
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
    private final FolderService folderService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String tag,
                       @RequestParam(required = false) Long folder,
                       @RequestParam(defaultValue = "false") boolean uncategorized,
                       @PageableDefault(size = 20, sort = "createdAt",
                               direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        boolean browseMode = folder == null && !uncategorized
                && !StringUtils.hasText(q) && !StringUtils.hasText(tag);
        model.addAttribute("papers", paperQueryService.search(q, tag, folder, uncategorized, pageable));
        model.addAttribute("q", q);
        model.addAttribute("tag", tag);
        model.addAttribute("folder", folder);
        model.addAttribute("uncategorized", uncategorized);
        model.addAttribute("browseMode", browseMode);
        model.addAttribute("selectedFolderName", folder == null ? null : folderService.findById(folder).getName());
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("folders", folderService.findAll());
        return "paper/list";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new PaperUploadRequest());
        }
        model.addAttribute("folders", folderService.findAll());
        return "paper/upload";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute("form") PaperUploadRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LabUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("folders", folderService.findAll());
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

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(name = "ids", required = false) java.util.List<Long> ids,
                             RedirectAttributes ra) {
        int count = paperService.softDeleteAll(ids == null ? java.util.List.of() : ids);
        ra.addFlashAttribute("message", count + "편을 휴지통으로 이동했습니다.");
        return "redirect:/papers";
    }

    @PostMapping("/folders")
    public String createFolder(@RequestParam String name, RedirectAttributes ra) {
        folderService.create(name);
        ra.addFlashAttribute("message", "폴더를 만들었습니다.");
        return "redirect:/papers";
    }
}
