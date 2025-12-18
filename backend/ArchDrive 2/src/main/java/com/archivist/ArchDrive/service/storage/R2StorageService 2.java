package com.archivist.ArchDrive.service.storage;

import com.archivist.ArchDrive.model.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
// import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
// import software.amazon.awssdk.transfer.s3.S3TransferManager;
// import software.amazon.awssdk.transfer.s3.model.Upload;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class R2StorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);
    // private static final ExecutorService uploadExecutor = Executors.newFixedThreadPool(4);

    @Autowired
    private S3Client s3Client;

    @Autowired
    // private S3TransferManager transferManager;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Value("${cloudflare.r2.accountId}")
    private String accountId;

    @Value("${cloudflare.r2.publicUrl:}")
    private String publicUrlBase;

    // @Override
    // public StoredFile uploadFile(MultipartFile file) {
    //     try {
    //         String originalFileName = file.getOriginalFilename();
    //         String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
    //         long contentLength = file.getSize();
            
    //         try (InputStream is = file.getInputStream()) {
    //             Upload upload = transferManager.upload(u -> u
    //                     .putObjectRequest(b -> b.bucket(bucketName)
    //                             .key(fileName)
    //                             .contentType(file.getContentType())
    //                             .contentLength(contentLength))
    //                     .requestBody(AsyncRequestBody.fromInputStream(is, contentLength, uploadExecutor))
    //             );
    //             upload.completionFuture().join(); // очікуємо завершення
    //         }

    //         // Generate public URL
    //         String publicUrl = generatePublicUrl(fileName);

    //         StoredFile storedFile = new StoredFile();
    //         storedFile.setId(fileName);
    //         storedFile.setFileName(originalFileName);
    //         storedFile.setUrl(publicUrl);
    //         storedFile.setSize(file.getSize());
    //         storedFile.setUploadedAt(LocalDateTime.now());

    //         return storedFile;
    //     } catch (Exception e) {
    //         log.error("Failed to upload file '{}' to R2: {}", file.getOriginalFilename(), e.getMessage(), e);
    //         throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
    //     }
    // }

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
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
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
                        String originalFileName = extractOriginalFileName(fileName);
                        String publicUrl = generatePublicUrl(fileName);

                        StoredFile storedFile = new StoredFile();
                        storedFile.setId(fileName);
                        storedFile.setFileName(originalFileName);
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

    private String generatePublicUrl(String fileName) {
        // If custom public URL base is configured, use it
        if (publicUrlBase != null && !publicUrlBase.isEmpty()) {
            return publicUrlBase.endsWith("/") 
                ? publicUrlBase + fileName 
                : publicUrlBase + "/" + fileName;
        }
        // Default R2 public URL format (requires custom domain setup)
        // For R2, you typically need to set up a custom domain or use presigned URLs
        return String.format("https://%s.r2.cloudflarestorage.com/%s/%s", accountId, bucketName, fileName);
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

