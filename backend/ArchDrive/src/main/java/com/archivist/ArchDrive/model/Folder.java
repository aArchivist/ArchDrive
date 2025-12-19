package com.archivist.ArchDrive.model;

import java.time.LocalDateTime;

public class Folder {
    private String id;
    private String name;
    private String path; // Full path (e.g., "documents/", "images/screenshots/")
    private LocalDateTime createdAt;
    private int fileCount; // Number of files in this folder

    public Folder() {
    }

    public Folder(String id, String name, String path, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.createdAt = createdAt;
        this.fileCount = 0;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }
}
