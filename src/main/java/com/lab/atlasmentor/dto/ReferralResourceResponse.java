package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import com.lab.atlasmentor.model.ReferralResource.ResourceType;
import com.lab.atlasmentor.model.ReferralResource.StorageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReferralResourceResponse {

    private Long id;
    private List<Long> ownerIds;
    private List<String> ownerNames;
    private OwnerType ownerType;
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
}