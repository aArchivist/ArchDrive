package com.archivist.ArchDrive.service.storage;

import com.archivist.ArchDrive.model.Folder;
import com.archivist.ArchDrive.model.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    StoredFile uploadFile(MultipartFile file);
    StoredFile uploadFile(MultipartFile file, String folder);
    Resource downloadFile(String fileName);
    List<StoredFile> listFiles();
    List<StoredFile> listFiles(String folder);
    List<Folder> listFolders();
    List<Folder> listFolders(String parentFolder);
    Folder createFolder(String folderName);
    void deleteFile(String fileName);
    void deleteFolder(String folderName);
}

