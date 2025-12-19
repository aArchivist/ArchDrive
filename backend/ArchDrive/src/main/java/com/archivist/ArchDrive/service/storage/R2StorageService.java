package com.archivist.ArchDrive.service.storage;

import com.archivist.ArchDrive.model.Folder;
import com.archivist.ArchDrive.model.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class R2StorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    @Autowired
    private S3Client s3Client;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Value("${cloudflare.r2.accountId}")
    private String accountId;

    @Value("${cloudflare.r2.accessKey}")
    private String accessKey;

    @Value("${cloudflare.r2.secretKey}")
    private String secretKey;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.publicUrl:}")
    private String publicUrlBase;

    @Override
    public StoredFile uploadFile(MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            System.out.println("Request details:");
            System.out.println("- Bucket: " + bucketName);
            System.out.println("- Key: " + fileName);
            System.out.println("- Content-Type: " + file.getContentType());
            System.out.println("- Content-Length: " + file.getSize());

            // Спробуємо спочатку прочитати файл повністю в пам'ять для тестування
            byte[] fileBytes = file.getBytes();
            System.out.println("File read into memory, size: " + fileBytes.length);

            // Спробуємо багато разів для мобільного інтернету - великі файли часто провалюються
            int maxRetries = 10; // Збільшено до 10 спроб для дуже великих файлів
            int attempt = 0;
            Exception lastException = null;

            while (attempt < maxRetries) {
                try {
                    attempt++;
                    System.out.println("Upload attempt " + attempt + " of " + maxRetries + " for large file (" + fileBytes.length + " bytes)");
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
                    System.out.println("Upload successful on attempt " + attempt + "!");
                    break; // Success, exit retry loop
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Upload attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt < maxRetries) {
                        // Ще довші паузи для великих файлів: 10s, 20s, 30s, 40s, 50s, 60s, 70s, 80s, 90s
                        int sleepTime = 10000 * attempt; // 10 секунд * номер спроби
                        System.out.println("Waiting " + (sleepTime/1000) + "s before retry " + (attempt + 1) + "...");
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Upload interrupted", ie);
                        }
                    }
                }
            }

            if (lastException != null && attempt == maxRetries) {
                throw new RuntimeException("Failed to upload after " + maxRetries + " attempts: " + lastException.getMessage(), lastException);
            }

            // Generate public URL
            String publicUrl = generatePublicUrl(fileName);

            StoredFile storedFile = new StoredFile();
            storedFile.setId(fileName);
            storedFile.setFileName(originalFileName);
            storedFile.setUrl(publicUrl);
            storedFile.setSize(file.getSize());
            storedFile.setUploadedAt(LocalDateTime.now());

            return storedFile;
        } catch (Exception e) {
            log.error("Failed to upload file '{}' to R2: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public StoredFile uploadFile(MultipartFile file, String folder) {
        try {
            String originalFileName = file.getOriginalFilename();
            String folderPath = (folder != null && !folder.isEmpty()) ? folder : "";
            String fileName = folderPath + UUID.randomUUID().toString() + "_" + originalFileName;

            System.out.println("Uploading file to R2:");
            System.out.println("- Original filename: " + originalFileName);
            System.out.println("- Folder path: '" + folderPath + "'");
            System.out.println("- Generated filename: " + fileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            System.out.println("Request details:");
            System.out.println("- Bucket: " + bucketName);
            System.out.println("- Key: " + fileName);
            System.out.println("- Folder: " + folderPath);
            System.out.println("- Content-Type: " + file.getContentType());
            System.out.println("- Content-Length: " + file.getSize());

            // Спробуємо спочатку прочитати файл повністю в пам'ять для тестування
            byte[] fileBytes = file.getBytes();
            System.out.println("File read into memory, size: " + fileBytes.length);

            // Спробуємо багато разів для мобільного інтернету - великі файли часто провалюються
            int maxRetries = 5; // Збільшено до 5 спроб
            int attempt = 0;
            Exception lastException = null;

            while (attempt < maxRetries) {
                try {
                    attempt++;
                    System.out.println("Upload attempt " + attempt + " of " + maxRetries + " for large file (" + fileBytes.length + " bytes)");
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
                    System.out.println("Upload successful on attempt " + attempt + "!");
                    break; // Success, exit retry loop
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("Upload attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt < maxRetries) {
                        // Ще довші паузи для великих файлів: 10s, 20s, 30s, 40s, 50s, 60s, 70s, 80s, 90s
                        int sleepTime = 10000 * attempt; // 10 секунд * номер спроби
                        System.out.println("Waiting " + (sleepTime/1000) + "s before retry " + (attempt + 1) + "...");
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Upload interrupted", ie);
                        }
                    }
                }
            }

            if (lastException != null && attempt == maxRetries) {
                throw new RuntimeException("Failed to upload after " + maxRetries + " attempts: " + lastException.getMessage(), lastException);
            }

            // Generate public URL
            String publicUrl = generatePublicUrl(fileName);

            StoredFile storedFile = new StoredFile();
            storedFile.setId(fileName);
            storedFile.setFileName(originalFileName);
            storedFile.setFolder(folderPath);
            storedFile.setUrl(publicUrl);
            storedFile.setSize(file.getSize());
            storedFile.setUploadedAt(LocalDateTime.now());

            return storedFile;
        } catch (Exception e) {
            log.error("Failed to upload file '{}' to R2: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource downloadFile(String fileName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            InputStream inputStream = s3Client.getObject(getObjectRequest);
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("Failed to download file '{}' from R2: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StoredFile> listFiles() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            return listResponse.contents().stream()
                    .map(s3Object -> {
                        String fileName = s3Object.key();
                        String folderPath = extractFolderPath(fileName);
                        String originalFileName = extractOriginalFileName(fileName);
                        String publicUrl = generatePublicUrl(fileName);

                        // Only include files that are actually in root (no folder path)
                        if (!folderPath.isEmpty()) {
                            return null; // Will be filtered out
                        }

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(fileName);
                        storedFile.setFileName(originalFileName);
                        storedFile.setFolder(folderPath); // Will be empty for root files
                        storedFile.setUrl(publicUrl);
                        storedFile.setSize(s3Object.size());
                        storedFile.setUploadedAt(s3Object.lastModified().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                        return storedFile;
                    })
                    .filter(Objects::nonNull) // Remove null entries (files in folders)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list files in R2 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to list files: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete file '{}' from R2: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StoredFile> listFiles(String folder) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);

            if (folder != null && !folder.isEmpty()) {
                requestBuilder.prefix(folder);
                System.out.println("Listing files with prefix: '" + folder + "'");
            } else {
                System.out.println("Listing all files (no prefix)");
            }

            ListObjectsV2Request listRequest = requestBuilder.build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            System.out.println("Found " + listResponse.contents().size() + " objects");

            return listResponse.contents().stream()
                    .map(s3Object -> {
                        String fileName = s3Object.key();
                        String folderPath = extractFolderPath(fileName);
                        String originalFileName = extractOriginalFileName(fileName);
                        String publicUrl = generatePublicUrl(fileName);

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(fileName);
                        storedFile.setFileName(originalFileName);
                        storedFile.setFolder(folderPath);
                        storedFile.setUrl(publicUrl);
                        storedFile.setSize(s3Object.size());
                        storedFile.setUploadedAt(s3Object.lastModified().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                        return storedFile;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list files in R2 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to list files: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Folder> listFolders() {
        return listFolders("");
    }

    @Override
    public List<Folder> listFolders(String parentFolder) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .delimiter("/");

            if (parentFolder != null && !parentFolder.isEmpty()) {
                requestBuilder.prefix(parentFolder);
            }

            ListObjectsV2Request listRequest = requestBuilder.build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            return listResponse.commonPrefixes().stream()
                    .map(prefix -> {
                        String folderPath = prefix.prefix();
                        String folderName = folderPath.endsWith("/")
                            ? folderPath.substring(0, folderPath.length() - 1)
                            : folderPath;

                        // Extract just the immediate folder name from the full path
                        String immediateName = folderName;
                        if (parentFolder != null && !parentFolder.isEmpty() && folderName.startsWith(parentFolder)) {
                            immediateName = folderName.substring(parentFolder.length());
                        }

                        // Count files in this folder
                        int fileCount = listFiles(folderPath).size();

                        Folder folder = new Folder();
                        folder.setId(folderPath);
                        folder.setName(immediateName);
                        folder.setPath(folderPath);
                        folder.setCreatedAt(LocalDateTime.now()); // R2 doesn't store folder creation time
                        folder.setFileCount(fileCount);

                        return folder;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list folders in R2 bucket '{}' with parent '{}': {}", bucketName, parentFolder, e.getMessage(), e);
            throw new RuntimeException("Failed to list folders: " + e.getMessage(), e);
        }
    }

    @Override
    public Folder createFolder(String folderName) {
        try {
            // In S3/R2, folders are created implicitly when a file is uploaded with a prefix
            // We'll create a placeholder object to ensure the folder appears in listings
            String folderPath = folderName.endsWith("/") ? folderName : folderName + "/";
            String placeholderKey = folderPath + ".keep"; // Hidden placeholder file

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(placeholderKey)
                    .contentType("application/x-directory")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.empty());

            Folder folder = new Folder();
            folder.setId(folderPath);
            folder.setName(folderName);
            folder.setPath(folderPath);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setFileCount(0);

            return folder;
        } catch (Exception e) {
            log.error("Failed to create folder '{}' in R2: {}", folderName, e.getMessage(), e);
            throw new RuntimeException("Failed to create folder: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFolder(String folderName) {
        try {
            String folderPath = folderName.endsWith("/") ? folderName : folderName + "/";

            // Delete all files in the folder
            List<StoredFile> filesInFolder = listFiles(folderPath);
            for (StoredFile file : filesInFolder) {
                deleteFile(file.getId());
            }

            // Delete the placeholder file if it exists
            String placeholderKey = folderPath + ".keep";
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(placeholderKey)
                        .build();
                s3Client.deleteObject(deleteRequest);
            } catch (Exception e) {
                // Placeholder might not exist, ignore
            }

        } catch (Exception e) {
            log.error("Failed to delete folder '{}' from R2: {}", folderName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete folder: " + e.getMessage(), e);
        }
    }

    private String generatePublicUrl(String fileName) {
        // For now, return direct R2 URL
        // To make downloads work, you need to either:
        // 1. Set up a custom domain in R2 and configure public access
        // 2. Or implement presigned URLs (requires additional AWS SDK dependencies)

        if (this.publicUrlBase != null && !this.publicUrlBase.isEmpty()) {
            return this.publicUrlBase.endsWith("/")
                ? this.publicUrlBase + fileName
                : this.publicUrlBase + "/" + fileName;
        }

        // Default R2 URL (requires public bucket access or custom domain)
        return String.format("https://%s.r2.cloudflarestorage.com/%s/%s", this.accountId, this.bucketName, fileName);
    }

    private String extractFolderPath(String fileName) {
        // Extract folder path (everything before the UUID)
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            String prefix = fileName.substring(0, underscoreIndex);
            // Find the last folder separator
            int lastSlashIndex = prefix.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                String folderPath = prefix.substring(0, lastSlashIndex + 1);
                System.out.println("Extracted folder path for '" + fileName + "': '" + folderPath + "'");
                return folderPath;
            }
        }
        System.out.println("No folder path found for '" + fileName + "'");
        return "";
    }

    private String extractOriginalFileName(String fileName) {
        // Remove UUID prefix if present
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0 && underscoreIndex < fileName.length() - 1) {
            return fileName.substring(underscoreIndex + 1);
        }
        return fileName;
    }
}

