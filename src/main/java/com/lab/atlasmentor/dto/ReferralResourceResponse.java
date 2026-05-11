package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import com.lab.atlasmentor.model.ReferralResource.ResourceType;
import com.lab.atlasmentor.model.ReferralResource.StorageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReferralResourceResponse {
    
    private Long id;
    private Long ownerId;
    private OwnerType ownerType;
    private String ownerName;
    private Long uploadedById;
    private String uploadedByName;
    private StorageType storageType;
    private String externalUrl;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private ResourceType resourceType;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    public ReferralResourceResponse() {}
    
    public ReferralResourceResponse(Long id, Long ownerId, OwnerType ownerType, String ownerName,
                                    Long uploadedById, String uploadedByName,
                                    StorageType storageType, String externalUrl, String filePath,
                                    String fileName, Long fileSize, String mimeType, ResourceType resourceType,
                                    String description, Boolean isActive, LocalDateTime createdAt,
                                    LocalDateTime updatedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.ownerName = ownerName;
        this.uploadedById = uploadedById;
        this.uploadedByName = uploadedByName;
        this.storageType = storageType;
        this.externalUrl = externalUrl;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.resourceType = resourceType;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }
}
