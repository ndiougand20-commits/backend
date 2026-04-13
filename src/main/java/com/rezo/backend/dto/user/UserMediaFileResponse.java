package com.rezo.backend.dto.user;

import com.rezo.entities.UserMediaFile;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserMediaFileResponse {

    private UUID id;
    private String category;
    private String originalFileName;
    private String contentType;
    private String fileUrl;
    private LocalDateTime createdAt;

    public static UserMediaFileResponse from(UserMediaFile file) {
        UserMediaFileResponse response = new UserMediaFileResponse();
        response.setId(file.getId());
        response.setCategory(file.getCategory());
        response.setOriginalFileName(file.getOriginalFileName());
        response.setContentType(file.getContentType());
        response.setFileUrl(file.getFileUrl());
        response.setCreatedAt(file.getCreatedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
