package com.lab.paperarchive.paper;

import com.lab.paperarchive.audit.AuditAction;
import com.lab.paperarchive.audit.AuditLogService;
import com.lab.paperarchive.storage.ReadablePaperStorage;
import com.lab.paperarchive.storage.StorageService;
import com.lab.paperarchive.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceAuditTest {

    @Mock PaperRepository paperRepository;
    @Mock PaperFileRepository paperFileRepository;
    @Mock TagRepository tagRepository;
    @Mock FolderRepository folderRepository;
    @Mock UserRepository userRepository;
    @Mock StorageService storageService;
    @Mock ReadablePaperStorage readablePaperStorage;
    @Mock AuditLogService auditLogService;

    @InjectMocks PaperService paperService;

    @Test
    void recordsPreviousAndNewFolderWhenMovingPaper() {
        Paper paper = Paper.builder().title("목재 논문").build();
        Folder targetFolder = Folder.of("지압강도");
        when(paperRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(paper));
        when(folderRepository.findById(2L)).thenReturn(Optional.of(targetFolder));

        paperService.moveToFolder(1L, 2L);

        verify(auditLogService).record(
                AuditAction.PAPER_MOVE,
                "PAPER",
                1L,
                "목재 논문 · 미분류 → 지압강도"
        );
    }
}
