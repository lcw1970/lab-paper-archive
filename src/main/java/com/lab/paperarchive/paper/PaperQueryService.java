package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.exception.BusinessException;
import com.lab.paperarchive.paper.dto.PaperListItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaperQueryService {

    private final PaperRepository paperRepository;

    @Transactional(readOnly = true)
    public Page<PaperListItem> search(String keyword, String tag, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String tg = StringUtils.hasText(tag) ? tag.trim() : "";
        return paperRepository.search(kw, tg, pageable).map(PaperListItem::from);
    }

    @Transactional(readOnly = true)
    public Paper findDetail(Long id) {
        Paper paper = paperRepository.findDetailWithTags(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 논문입니다."));
        paperRepository.findDetailWithFiles(id);   // 같은 인스턴스에 files 를 채운다
        return paper;
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return paperRepository.countByDeletedAtIsNull();
    }
}
