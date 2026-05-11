package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.ReferralResourceRequest;
import com.lab.atlasmentor.dto.ReferralResourceResponse;
import com.lab.atlasmentor.model.ReferralResource;
import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.ReferralAssignmentRepository;
import com.lab.atlasmentor.repository.ReferralResourceRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReferralResourceService {

    @Autowired
    private ReferralResourceRepository referralResourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferralAssignmentRepository referralAssignmentRepository;

    @Transactional
    public List<ReferralResourceResponse> createResource(ReferralResourceRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Check permissions - only ADMIN, MANAGER, BRANCH_PARTNER can create
        if (!canManageResources(currentUser)) {
            throw new RuntimeException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can create resources.");
        }
        
        User uploadedBy = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        List<ReferralResourceResponse> responses = new ArrayList<>();
        
        for (Long ownerId : request.getOwnerIds()) {
            // Verify owner exists and has correct role
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + ownerId));
            
            // Validate owner type matches the user's role
            if (request.getOwnerType() == OwnerType.REFERRAL && !owner.hasRole("REFERRAL")) {
                throw new RuntimeException("User is not a referral: " + ownerId);
            }
            if (request.getOwnerType() == OwnerType.COMPANY && !owner.hasRole("COMPANY")) {
                throw new RuntimeException("User is not a company: " + ownerId);
            }
            
            // Branch-based access control for non-admin users
            if (!currentUser.isAdmin() && currentUser.getBranchId() != null) {
                if (owner.getBranchId() == null || !owner.getBranchId().equals(currentUser.getBranchId())) {
                    throw new RuntimeException("Access denied. You can only manage resources for owners in your branch. Owner: " + ownerId);
                }
            }
            
            ReferralResource resource = new ReferralResource();
            resource.setOwner(owner);
            resource.setOwnerType(request.getOwnerType());
            resource.setUploadedBy(uploadedBy);
            resource.setStorageType(request.getStorageType());
            resource.setExternalUrl(request.getExternalUrl());
            resource.setFilePath(request.getFilePath());
            resource.setFileName(request.getFileName());
            resource.setFileSize(request.getFileSize());
            resource.setMimeType(request.getMimeType());
            resource.setResourceType(request.getResourceType());
            resource.setDescription(request.getDescription());
            resource.setExpiresAt(request.getExpiresAt());
            resource.setIsActive(true);
            resource.setCreatedBy(currentUser.getUserId());
            resource.setUpdatedBy(currentUser.getUserId());
            
            ReferralResource savedResource = referralResourceRepository.save(resource);
            responses.add(convertToResponse(savedResource));
            
            log.info("Resource created: id={}, ownerId={}, uploadedBy={}", 
                    savedResource.getId(), savedResource.getOwnerId(), savedResource.getUploadedById());
        }
        
        return responses;
    }

    @Transactional
    public ReferralResourceResponse updateResource(Long resourceId, ReferralResourceRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        ReferralResource resource = referralResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + resourceId));
        
        // Check permissions
        if (!canManageResources(currentUser)) {
            throw new RuntimeException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can update resources.");
        }
        
        // Only admin or the original uploader can update
        if (!currentUser.isAdmin() && !resource.getUploadedById().equals(currentUser.getUserId())) {
            throw new RuntimeException("Access denied. You can only update resources you uploaded.");
        }
        
        // Branch-based access control
        if (!currentUser.isAdmin() && currentUser.getBranchId() != null) {
            User owner = resource.getOwner();
            if (owner.getBranchId() == null || !owner.getBranchId().equals(currentUser.getBranchId())) {
                throw new RuntimeException("Access denied. You can only manage resources for owners in your branch.");
            }
        }
        
        // Update fields
        resource.setStorageType(request.getStorageType());
        resource.setExternalUrl(request.getExternalUrl());
        resource.setFilePath(request.getFilePath());
        resource.setFileName(request.getFileName());
        resource.setFileSize(request.getFileSize());
        resource.setMimeType(request.getMimeType());
        resource.setResourceType(request.getResourceType());
        resource.setDescription(request.getDescription());
        resource.setExpiresAt(request.getExpiresAt());
        resource.setUpdatedBy(currentUser.getUserId());
        
        ReferralResource updatedResource = referralResourceRepository.save(resource);
        
        log.info("Resource updated: id={}, updatedBy={}", updatedResource.getId(), currentUser.getUserId());
        
        return convertToResponse(updatedResource);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        ReferralResource resource = referralResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + resourceId));
        
        // Check permissions
        if (!canManageResources(currentUser)) {
            throw new RuntimeException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can delete resources.");
        }
        
        // Only admin or the original uploader can delete
        if (!currentUser.isAdmin() && !resource.getUploadedById().equals(currentUser.getUserId())) {
            throw new RuntimeException("Access denied. You can only delete resources you uploaded.");
        }
        
        // Branch-based access control
        if (!currentUser.isAdmin() && currentUser.getBranchId() != null) {
            User owner = resource.getOwner();
            if (owner.getBranchId() == null || !owner.getBranchId().equals(currentUser.getBranchId())) {
                throw new RuntimeException("Access denied. You can only manage resources for owners in your branch.");
            }
        }
        
        // Soft delete - mark as inactive
        resource.setIsActive(false);
        resource.setUpdatedBy(currentUser.getUserId());
        referralResourceRepository.save(resource);
        
        log.info("Resource soft deleted: id={}, deletedBy={}", resourceId, currentUser.getUserId());
    }

    @Transactional(readOnly = true)
    public ReferralResourceResponse getResourceById(Long resourceId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        ReferralResource resource = referralResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + resourceId));
        
        // Check access permissions
        if (!canAccessResource(currentUser, resource)) {
            throw new RuntimeException("Access denied. You do not have permission to view this resource.");
        }
        
        return convertToResponse(resource);
    }

    @Transactional(readOnly = true)
    public List<ReferralResourceResponse> getResourcesByOwnerId(Long ownerId, OwnerType ownerType) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Verify owner exists and has correct role
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + ownerId));
        
        if (ownerType == OwnerType.REFERRAL && !owner.hasRole("REFERRAL")) {
            throw new RuntimeException("User is not a referral");
        }
        if (ownerType == OwnerType.COMPANY && !owner.hasRole("COMPANY")) {
            throw new RuntimeException("User is not a company");
        }
        
        // Check access permissions
        if (!canAccessOwnerResources(currentUser, owner)) {
            throw new RuntimeException("Access denied. You do not have permission to view resources for this owner.");
        }
        
        List<ReferralResource> resources;
        
        // REFERRAL and COMPANY can only see active resources assigned to them
        if (currentUser.getUserId().equals(ownerId)) {
            resources = referralResourceRepository.findByOwner_IdAndOwnerTypeAndIsActiveTrue(ownerId, ownerType);
        } else {
            // Admin, Manager can see all resources
            resources = referralResourceRepository.findByOwner_IdAndOwnerType(ownerId, ownerType);
        }
        
        return resources.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReferralResourceResponse> getAllResources(int page, int size, Long ownerId, OwnerType ownerType, String resourceType) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Only ADMIN, MANAGER, BRANCH_PARTNER can access all resources
        if (!canManageResources(currentUser)) {
            throw new RuntimeException("Access denied. This API is only available for ADMIN, MANAGER, and BRANCH_PARTNER roles.");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        List<ReferralResource> allResources = referralResourceRepository.findAll();
        
        // Apply filters - only show active resources
        List<ReferralResource> filteredResources = allResources.stream()
                .filter(r -> r.getIsActive())
                .filter(r -> ownerId == null || r.getOwnerId().equals(ownerId))
                .filter(r -> ownerType == null || r.getOwnerType() == ownerType)
                .filter(r -> resourceType == null || r.getResourceType().name().equalsIgnoreCase(resourceType))
                .filter(r -> {
                    // Branch-based filtering for non-admin
                    if (currentUser.isAdmin()) return true;
                    if (currentUser.getBranchId() == null) return true;
                    User owner = r.getOwner();
                    return owner.getBranchId() != null && owner.getBranchId().equals(currentUser.getBranchId());
                })
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, filteredResources.size());
        List<ReferralResource> paginatedList = start >= filteredResources.size() 
                ? List.of() 
                : filteredResources.subList(start, end);
        
        List<ReferralResourceResponse> responses = paginatedList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return PageResponse.of(responses, page, size, filteredResources.size());
    }

    @Transactional(readOnly = true)
    public List<ReferralResourceResponse> getMyResources() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        
        // Only REFERRAL and COMPANY roles can access this
        String role = currentUser.getRole();
        if (!role.equalsIgnoreCase("REFERRAL") && !role.equalsIgnoreCase("COMPANY")) {
            throw new RuntimeException("Access denied. This API is only available for REFERRAL and COMPANY roles.");
        }
        
        OwnerType ownerType = role.equalsIgnoreCase("REFERRAL") ? OwnerType.REFERRAL : OwnerType.COMPANY;
        
        List<ReferralResource> resources = referralResourceRepository.findByOwner_IdAndOwnerTypeAndIsActiveTrue(
                currentUser.getUserId(), ownerType);
        
        return resources.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private boolean canManageResources(CustomUserDetails user) {
        return user.isAdmin() || user.isManager() || user.getRole().equalsIgnoreCase("BRANCH_PARTNER");
    }

    private boolean canAccessResource(CustomUserDetails user, ReferralResource resource) {
        // Admin can access all
        if (user.isAdmin()) return true;
        
        // Manager/Branch Partner can access resources in their branch
        if (user.isManager() || user.getRole().equalsIgnoreCase("BRANCH_PARTNER")) {
            User owner = resource.getOwner();
            if (user.getBranchId() != null && owner.getBranchId() != null) {
                return owner.getBranchId().equals(user.getBranchId());
            }
            return true; // If no branch assigned, allow access
        }
        
        // Referral can only access their own active resources
        if (user.getRole().equalsIgnoreCase("REFERRAL")) {
            return resource.getOwnerId().equals(user.getUserId()) && resource.getIsActive() && resource.getOwnerType() == OwnerType.REFERRAL;
        }
        
        // Company can only access their own active resources
        if (user.getRole().equalsIgnoreCase("COMPANY")) {
            return resource.getOwnerId().equals(user.getUserId()) && resource.getIsActive() && resource.getOwnerType() == OwnerType.COMPANY;
        }
        
        return false;
    }

    private boolean canAccessOwnerResources(CustomUserDetails user, User owner) {
        // Admin can access all
        if (user.isAdmin()) return true;
        
        // Manager/Branch Partner can access owners in their branch
        if (user.isManager() || user.getRole().equalsIgnoreCase("BRANCH_PARTNER")) {
            if (user.getBranchId() != null && owner.getBranchId() != null) {
                return owner.getBranchId().equals(user.getBranchId());
            }
            return true;
        }
        
        // Referral can only access their own resources
        if (user.getRole().equalsIgnoreCase("REFERRAL")) {
            return owner.getId().equals(user.getUserId()) && owner.hasRole("REFERRAL");
        }
        
        // Company can only access their own resources
        if (user.getRole().equalsIgnoreCase("COMPANY")) {
            return owner.getId().equals(user.getUserId()) && owner.hasRole("COMPANY");
        }
        
        return false;
    }

    private ReferralResourceResponse convertToResponse(ReferralResource resource) {
        String uploadedByName = resource.getUploadedBy() != null 
                ? resource.getUploadedBy().getFullName() 
                : "Unknown";
        
        String ownerName = resource.getOwner() != null
                ? resource.getOwner().getFullName()
                : "Unknown";
        
        return new ReferralResourceResponse(
                resource.getId(),
                resource.getOwnerId(),
                resource.getOwnerType(),
                ownerName,
                resource.getUploadedById(),
                uploadedByName,
                resource.getStorageType(),
                resource.getExternalUrl(),
                resource.getFilePath(),
                resource.getFileName(),
                resource.getFileSize(),
                resource.getMimeType(),
                resource.getResourceType(),
                resource.getDescription(),
                resource.getIsActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt(),
                resource.getExpiresAt()
        );
    }
}
