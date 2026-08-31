package com.lab.paperarchive.common.exception;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, RedirectAttributes ra,
                                 HttpServletRequest request) {
        log.info("업무 오류: {}", e.getMessage());
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:" + previousLocalPath(request.getHeader("Referer"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSize(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "파일 크기가 100MB를 초과했습니다.");
        return "redirect:/papers/upload";
    }

    /** 외부 주소로의 리다이렉트는 허용하지 않는다. */
    private String previousLocalPath(String referer) {
        if (referer == null || referer.isBlank()) return "/papers";
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "/papers";
            }
            return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException ignored) {
            return "/papers";
        }
    }
}
