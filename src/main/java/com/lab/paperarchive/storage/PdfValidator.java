package com.lab.paperarchive.storage;

import com.lab.paperarchive.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Component
public class PdfValidator {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF

    /** 확장자가 아닌 실제 바이트로 PDF 여부를 판정한다. */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("파일이 비어 있습니다.");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(4);
            if (!Arrays.equals(head, PDF_MAGIC)) {
                throw new BusinessException("PDF 파일만 업로드할 수 있습니다.");
            }
        } catch (IOException e) {
            throw new BusinessException("파일을 읽을 수 없습니다.");
        }
    }
}
