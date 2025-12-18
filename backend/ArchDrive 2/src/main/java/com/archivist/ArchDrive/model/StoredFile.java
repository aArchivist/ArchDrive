package com.archivist.ArchDrive.model;

import java.time.LocalDateTime;

public class StoredFile {
    private String id;
    private String fileName;
    private String url;
    private long size;
    private LocalDateTime uploadedAt;

    public StoredFile() {
    }

    public StoredFile(String id, String fileName, String url, long size, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.url = url;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

