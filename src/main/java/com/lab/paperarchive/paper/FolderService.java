package com.lab.paperarchive.paper;

import com.lab.paperarchive.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        return folderRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Folder findById(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 폴더입니다."));
    }

    @Transactional
    public void create(String rawName) {
        String name = StringUtils.hasText(rawName) ? rawName.trim() : "";
        if (name.isBlank()) {
            throw new BusinessException("폴더 이름을 입력해 주세요.");
        }
        if (name.length() > 100) {
            throw new BusinessException("폴더 이름은 100자 이내여야 합니다.");
        }
        if (folderRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new BusinessException("같은 이름의 폴더가 이미 있습니다.");
        }
        folderRepository.save(Folder.of(name));
    }
}
