package com.lab.paperarchive.paper;

import com.lab.paperarchive.paper.dto.PaperUploadRequest;
import com.lab.paperarchive.paper.dto.PaperUpdateRequest;
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
        model.addAttribute("folderSummaries", folderService.findAllWithPaperCount());
        model.addAttribute("uncategorizedCount", folderService.countUncategorized());
        return "paper/list";
    }

    @GetMapping("/trash")
    public String trash(@PageableDefault(size = 20, sort = "deletedAt",
            direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("papers", paperQueryService.findDeleted(pageable));
        return "paper/trash";
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
        model.addAttribute("folders", folderService.findAll());
        return "paper/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("paperId", id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", PaperUpdateRequest.from(paperQueryService.findDetail(id)));
        }
        return "paper/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") PaperUpdateRequest form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("paperId", id);
            return "paper/edit";
        }
        paperService.updateMetadata(id, form);
        ra.addFlashAttribute("message", "논문 정보를 수정했습니다.");
        return "redirect:/papers/" + id;
    }

    @PostMapping("/{id}/folder")
    public String moveFolder(@PathVariable Long id,
                             @RequestParam(required = false) Long folderId,
                             RedirectAttributes ra) {
        paperService.moveToFolder(id, folderId);
        ra.addFlashAttribute("message", "논문 폴더를 변경했습니다.");
        return "redirect:/papers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        paperService.softDelete(id);
        ra.addFlashAttribute("message", "휴지통으로 이동했습니다.");
        return "redirect:/papers";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes ra) {
        paperService.restore(id);
        ra.addFlashAttribute("message", "논문을 복원했습니다.");
        return "redirect:/papers/" + id;
    }

    @PostMapping("/{id}/permanent-delete")
    public String deletePermanently(@PathVariable Long id, RedirectAttributes ra) {
        paperService.deletePermanently(id);
        ra.addFlashAttribute("message", "논문과 PDF 파일을 영구 삭제했습니다.");
        return "redirect:/papers/trash";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(name = "ids", required = false) java.util.List<Long> ids,
                             RedirectAttributes ra) {
        int count = paperService.softDeleteAll(ids == null ? java.util.List.of() : ids);
        ra.addFlashAttribute("message", count + "편을 휴지통으로 이동했습니다.");
        return "redirect:/papers";
    }

    @PostMapping("/bulk-restore")
    public String bulkRestore(@RequestParam(name = "ids", required = false) java.util.List<Long> ids,
                              RedirectAttributes ra) {
        int count = paperService.restoreAll(ids == null ? java.util.List.of() : ids);
        ra.addFlashAttribute("message", count + "편을 복원했습니다.");
        return "redirect:/papers/trash";
    }

    @PostMapping("/bulk-permanent-delete")
    public String bulkDeletePermanently(@RequestParam(name = "ids", required = false) java.util.List<Long> ids,
                                         RedirectAttributes ra) {
        int count = paperService.deleteAllPermanently(ids == null ? java.util.List.of() : ids);
        ra.addFlashAttribute("message", count + "편과 연결된 PDF 파일을 영구 삭제했습니다.");
        return "redirect:/papers/trash";
    }

    @PostMapping("/bulk-move")
    public String bulkMove(@RequestParam(name = "ids", required = false) java.util.List<Long> ids,
                           @RequestParam(required = false) Long folderId,
                           RedirectAttributes ra) {
        int count = paperService.moveAllToFolder(ids == null ? java.util.List.of() : ids, folderId);
        ra.addFlashAttribute("message", count + "편의 폴더를 변경했습니다.");
        return "redirect:/papers";
    }

    @PostMapping("/folders")
    public String createFolder(@RequestParam String name, RedirectAttributes ra) {
        folderService.create(name);
        ra.addFlashAttribute("message", "폴더를 만들었습니다.");
        return "redirect:/papers";
    }

    @PostMapping("/folders/{id}/delete")
    public String deleteFolder(@PathVariable Long id, RedirectAttributes ra) {
        folderService.delete(id);
        ra.addFlashAttribute("message", "폴더를 삭제했습니다. 안의 논문은 미분류로 이동했습니다.");
        return "redirect:/papers";
    }
}
