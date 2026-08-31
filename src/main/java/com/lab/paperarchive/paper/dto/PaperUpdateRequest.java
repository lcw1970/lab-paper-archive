package com.lab.paperarchive.paper.dto;

import com.lab.paperarchive.paper.Paper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

/** 논문 상세 화면에서 수정할 수 있는 메타데이터만 담는다. */
@Getter
@Setter
public class PaperUpdateRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 500, message = "제목은 500자 이내여야 합니다.")
    private String title;

    @Size(max = 500)
    private String authors;

    @Size(max = 300)
    private String tags;

    private String memo;

    public static PaperUpdateRequest from(Paper paper) {
        PaperUpdateRequest request = new PaperUpdateRequest();
        request.title = paper.getTitle();
        request.authors = paper.getAuthors();
        request.memo = paper.getMemo();
        request.tags = paper.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.joining(", "));
        return request;
    }
}
