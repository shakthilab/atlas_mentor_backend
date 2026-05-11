package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "referral_resources",
       indexes = {

           @Index(name = "idx_referral_resources_owner_id", columnList = "owner_id"),
           @Index(name = "idx_referral_resources_owner_type", columnList = "owner_type"),
           @Index(name = "idx_referral_resources_storage_type", columnList = "storage_type"),
           @Index(name = "idx_referral_resources_is_active", columnList = "is_active")
       })
@Data
@EqualsAndHashCode(callSuper = false)
public class ReferralResource extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User owner;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User uploadedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;
    
    @Column(name = "external_url", length = 1000)
    private String externalUrl;
    
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    @Column(name = "file_name", length = 255)
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;
    
    public ReferralResource() {}
    
    public ReferralResource(User owner, OwnerType ownerType, User uploadedBy, StorageType storageType, ResourceType resourceType) {
        this.owner = owner;
        this.ownerType = ownerType;
        this.uploadedBy = uploadedBy;
        this.storageType = storageType;
        this.resourceType = resourceType;
    }
    
    // Convenience methods
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
    
    public void setOwnerId(Long ownerId) {
        if (ownerId != null) {
            this.owner = new User();
            this.owner.setId(ownerId);
        }
    }
    
    public Long getUploadedById() {
        return uploadedBy != null ? uploadedBy.getId() : null;
    }
    
    public void setUploadedById(Long uploadedById) {
        if (uploadedById != null) {
            this.uploadedBy = new User();
            this.uploadedBy.setId(uploadedById);
        }
    }
    
    // Enums
    public enum StorageType {
        GOOGLE_DRIVE,
        ONEDRIVE,
        DROPBOX,
        S3_UPLOAD,
        OTHER
    }
    
    public enum ResourceType {
        DOCUMENT,
        IMAGE,
        VIDEO,
        AUDIO,
        SPREADSHEET,
        PRESENTATION,
        LINK,
        OTHER
    }
    
    public enum OwnerType {
        REFERRAL,
        COMPANY
    }
}
