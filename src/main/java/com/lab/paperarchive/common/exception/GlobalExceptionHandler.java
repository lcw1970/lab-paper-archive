package com.lab.paperarchive.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/papers";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSize(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "파일 크기가 100MB를 초과했습니다.");
        return "redirect:/papers/upload";
    }
}
