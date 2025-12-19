package com.archivist.ArchDrive.controllers;

import com.archivist.ArchDrive.model.Folder;
import com.archivist.ArchDrive.model.StoredFile;
import com.archivist.ArchDrive.service.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "https://arch-drive.vercel.app/", "https://arch-drive-aarchivists-projects.vercel.app/"})
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<StoredFile> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String folder) {
        try {
            StoredFile storedFile;
            if (folder != null && !folder.trim().isEmpty()) {
                storedFile = fileStorageService.uploadFile(file, folder);
            } else {
                storedFile = fileStorageService.uploadFile(file);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(storedFile);
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<StoredFile>> getAllFiles(@RequestParam(required = false) String folder) {
        try {
            List<StoredFile> files;
            if (folder != null && !folder.trim().isEmpty()) {
                files = fileStorageService.listFiles(folder);
            } else {
                files = fileStorageService.listFiles();
            }
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("List files failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<List<Folder>> getFolders(@RequestParam(required = false) String parent) {
        try {
            List<Folder> folders;
            if (parent != null && !parent.trim().isEmpty()) {
                folders = fileStorageService.listFolders(parent);
            } else {
                folders = fileStorageService.listFolders();
            }
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            log.error("List folders failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> getFile(@RequestParam("fileName") String fileName) {
        try {
            Resource resource = fileStorageService.downloadFile(fileName);

            // Extract original filename for Content-Disposition header
            String originalFileName = fileName;
            if (fileName.contains("_")) {
                originalFileName = fileName.substring(fileName.indexOf("_") + 1);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Download failed for {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<Resource> previewFile(@RequestParam("fileName") String fileName) {
        try {
            Resource resource = fileStorageService.downloadFile(fileName);

            // Determine content type based on file extension
            String contentType = determineContentType(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Preview failed for {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String fileName) {
        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        switch (extension) {
            // Images
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "bmp":
                return "image/bmp";
            case "ico":
                return "image/x-icon";

            // Text files
            case "txt":
                return "text/plain;charset=UTF-8";
            case "md":
                return "text/markdown;charset=UTF-8";
            case "json":
                return "application/json;charset=UTF-8";
            case "xml":
                return "application/xml;charset=UTF-8";
            case "html":
            case "htm":
                return "text/html;charset=UTF-8";
            case "css":
                return "text/css;charset=UTF-8";
            case "js":
                return "application/javascript;charset=UTF-8";
            case "ts":
                return "application/typescript;charset=UTF-8";
            case "java":
                return "text/x-java-source;charset=UTF-8";
            case "py":
                return "text/x-python;charset=UTF-8";
            case "sql":
                return "application/sql;charset=UTF-8";
            case "yaml":
            case "yml":
                return "application/x-yaml;charset=UTF-8";

            // Documents
            case "pdf":
                return "application/pdf";

            // Video
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";

            // Audio
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "aac":
                return "audio/aac";
            case "flac":
                return "audio/flac";

            default:
                return "application/octet-stream";
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("fileName") String fileName) {
        try {
            fileStorageService.deleteFile(fileName);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Delete failed for {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/folders")
    public ResponseEntity<Folder> createFolder(@RequestParam("name") String folderName, @RequestParam(required = false) String parent) {
        try {
            String fullFolderName = folderName;
            if (parent != null && !parent.trim().isEmpty()) {
                // Remove trailing slash from parent and add it to folder name
                String parentPath = parent.endsWith("/") ? parent.substring(0, parent.length() - 1) : parent;
                fullFolderName = parentPath + "/" + folderName;
            }
            Folder folder = fileStorageService.createFolder(fullFolderName);
            return ResponseEntity.status(HttpStatus.CREATED).body(folder);
        } catch (Exception e) {
            log.error("Create folder failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/folders/{folderName}")
    public ResponseEntity<Void> deleteFolder(@PathVariable String folderName) {
        try {
            fileStorageService.deleteFolder(folderName);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Delete folder failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

