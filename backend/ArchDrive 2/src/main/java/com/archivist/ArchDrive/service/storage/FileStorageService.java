package com.archivist.ArchDrive.service.storage;

import com.archivist.ArchDrive.model.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    StoredFile uploadFile(MultipartFile file);
    Resource downloadFile(String fileName);
    List<StoredFile> listFiles();
    void deleteFile(String fileName);
}

