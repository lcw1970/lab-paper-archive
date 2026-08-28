package com.lab.paperarchive.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PaperUploadRequest {

    private MultipartFile file;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 500, message = "제목은 500자 이내여야 합니다.")
    private String title;

    @Size(max = 500)
    private String authors;

    @Size(max = 300)
    private String tags;

    private String memo;
}
