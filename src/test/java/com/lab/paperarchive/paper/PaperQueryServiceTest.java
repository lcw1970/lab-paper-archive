package com.lab.paperarchive.paper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperQueryServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Test
    void searchesWithBothComposedAndDecomposedKorean() {
        PaperQueryService service = new PaperQueryService(paperRepository);
        Pageable pageable = PageRequest.of(0, 20);
        when(paperRepository.search(anyString(), anyString(), anyString(), isNull(), anyBoolean(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        service.search("국내산", null, null, false, pageable);

        ArgumentCaptor<String> nfc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nfd = ArgumentCaptor.forClass(String.class);
        verify(paperRepository).search(nfc.capture(), nfd.capture(), anyString(), isNull(), anyBoolean(), eq(pageable));

        assertThat(nfc.getValue()).isEqualTo(Normalizer.normalize("국내산", Normalizer.Form.NFC));
        assertThat(nfd.getValue()).isEqualTo(Normalizer.normalize("국내산", Normalizer.Form.NFD));
        assertThat(nfd.getValue()).isNotEqualTo(nfc.getValue());
    }
}
