package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import com.lab.atlasmentor.model.ReferralResource.ResourceType;
import com.lab.atlasmentor.model.ReferralResource.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReferralResourceRequest {
    
    @NotEmpty(message = "At least one Owner ID is required")
    private List<Long> ownerIds;
    
    @NotNull(message = "Owner type is required")
    private OwnerType ownerType;
    
    @NotNull(message = "Storage type is required")
    private StorageType storageType;
    
    @NotBlank(message = "External URL is required")
    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    private String externalUrl;
    
    @Size(max = 500, message = "File path must not exceed 500 characters")
    private String filePath;
    
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;
    
    private Long fileSize;
    
    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;
    
    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private LocalDateTime expiresAt;
}
