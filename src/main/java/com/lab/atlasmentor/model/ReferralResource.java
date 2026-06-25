package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

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
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "referral_resource_owners",
        joinColumns = @JoinColumn(name = "resource_id"),
        inverseJoinColumns = @JoinColumn(name = "owner_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<User> owners = new ArrayList<>();
    
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
    
    public Long getUploadedById() {
        return uploadedBy != null ? uploadedBy.getId() : null;
    }
    
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
        COMPANY,
        SENIOR_COUNSELLOR,
        JUNIOR_COUNSELLOR,
        VIDEO_EDITOR,
        COUNSELLOR,
        MANAGER,
        BRANCH_PARTNER,
        ADMINISTRATIVE_ASSISTANT
    }
}
