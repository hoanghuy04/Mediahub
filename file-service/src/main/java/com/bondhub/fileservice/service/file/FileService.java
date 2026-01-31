package com.bondhub.fileservice.service.file;

import com.bondhub.fileservice.dto.response.file.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {
    FileUploadResponse uploadFile(MultipartFile file) throws IOException;

    byte[] downloadFile(String key);
}
