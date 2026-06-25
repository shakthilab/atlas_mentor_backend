package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ReferralResourceResponse createResource(ReferralResourceRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        if (!canManageResources(currentUser)) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, BRANCH_PARTNER, or ADMINISTRATIVE_ASSISTANT can create resources.");
        }

        User uploadedBy = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<User> owners = request.getOwnerIds().stream().map(ownerId -> {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + ownerId));

            if (!currentUser.isAdmin() && !currentUser.isManager() && currentUser.getBranchId() != null) {
                if (owner.getBranchId() == null || !owner.getBranchId().equals(currentUser.getBranchId())) {
                    throw new BusinessException("Access denied. You can only manage resources for owners in your branch. Owner: " + ownerId);
                }
            }

            return owner;
        }).collect(Collectors.toList());

        ReferralResource resource = new ReferralResource();
        resource.setOwners(owners);
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

        log.info("Resource created: id={}, owners={}, uploadedBy={}",
                savedResource.getId(),
                owners.stream().map(User::getId).collect(Collectors.toList()),
                savedResource.getUploadedById());

        return convertToResponse(savedResource);
    }

    @Transactional
    public ReferralResourceResponse updateResource(Long resourceId, ReferralResourceRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        ReferralResource resource = referralResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + resourceId));

        if (!canManageResources(currentUser)) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can update resources.");
        }

        if (!currentUser.isAdmin() && !currentUser.isManager() && !resource.getUploadedById().equals(currentUser.getUserId())) {
            throw new BusinessException("Access denied. You can only update resources you uploaded.");
        }

        if (!currentUser.isAdmin() && currentUser.getBranchId() != null) {
            boolean anyOwnerInBranch = resource.getOwners().stream().anyMatch(o ->
                    o.getBranchId() != null && o.getBranchId().equals(currentUser.getBranchId()));
            if (!anyOwnerInBranch) {
                throw new BusinessException("Access denied. You can only manage resources for owners in your branch.");
            }
        }

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

        // Update owners if provided
        if (request.getOwnerIds() != null && !request.getOwnerIds().isEmpty()) {
            List<User> owners = request.getOwnerIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + id)))
                    .collect(Collectors.toList());
            resource.setOwners(owners);
        }

        ReferralResource updatedResource = referralResourceRepository.save(resource);
        log.info("Resource updated: id={}, updatedBy={}", updatedResource.getId(), currentUser.getUserId());

        return convertToResponse(updatedResource);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        ReferralResource resource = referralResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + resourceId));

        if (!canManageResources(currentUser)) {
            throw new BusinessException("Access denied. Only ADMIN, MANAGER, or BRANCH_PARTNER can delete resources.");
        }

        if (!currentUser.isAdmin() && !currentUser.isManager() && !resource.getUploadedById().equals(currentUser.getUserId())) {
            throw new BusinessException("Access denied. You can only delete resources you uploaded.");
        }

        if (!currentUser.isAdmin() && currentUser.getBranchId() != null) {
            boolean anyOwnerInBranch = resource.getOwners().stream().anyMatch(o ->
                    o.getBranchId() != null && o.getBranchId().equals(currentUser.getBranchId()));
            if (!anyOwnerInBranch) {
                throw new BusinessException("Access denied. You can only manage resources for owners in your branch.");
            }
        }

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

        if (!canAccessResource(currentUser, resource)) {
            throw new BusinessException("Access denied. You do not have permission to view this resource.");
        }

        return convertToResponse(resource);
    }

    @Transactional(readOnly = true)
    public List<ReferralResourceResponse> getResourcesByOwnerId(Long ownerId, OwnerType ownerType) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + ownerId));

        if (!canAccessOwnerResources(currentUser, owner)) {
            throw new BusinessException("Access denied. You do not have permission to view resources for this owner.");
        }

        List<ReferralResource> resources;
        if (currentUser.getUserId().equals(ownerId)) {
            resources = referralResourceRepository.findByOwner_IdAndOwnerTypeAndIsActiveTrue(ownerId, ownerType);
        } else {
            resources = referralResourceRepository.findByOwner_IdAndOwnerType(ownerId, ownerType);
        }

        return resources.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReferralResourceResponse> getAllResources(int page, int size, Long ownerId, OwnerType ownerType, String resourceType) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        String role = currentUser.getRole();

        List<ReferralResource> filteredResources = referralResourceRepository.findAll().stream()
                .filter(r -> r.getIsActive())
                .filter(r -> ownerId == null || r.getOwners().stream().anyMatch(o -> o.getId().equals(ownerId)))
                .filter(r -> ownerType == null || r.getOwnerType() == ownerType)
                .filter(r -> resourceType == null || r.getResourceType().name().equalsIgnoreCase(resourceType))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .filter(r -> {
                    // ADMIN sees everything
                    if (currentUser.isAdmin()) return true;

                    // MANAGER, BRANCH_PARTNER, ADMINISTRATIVE_ASSISTANT see their branch's resources
                    if (currentUser.isManager()) {
                        if (currentUser.getBranchId() == null) return true;
                        return r.getOwners().stream().anyMatch(o ->
                                o.getBranchId() != null && o.getBranchId().equals(currentUser.getBranchId()));
                    }

                    // Everyone else sees only resources they are assigned to (in owners list)
                    return r.getOwners().stream().anyMatch(o -> o.getId().equals(currentUser.getUserId()));
                })
                .collect(Collectors.toList());

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

        // All non-admin, non-manager roles see only resources assigned to them
        return referralResourceRepository.findAll().stream()
                .filter(r -> r.getIsActive())
                .filter(r -> r.getOwners().stream().anyMatch(o -> o.getId().equals(currentUser.getUserId())))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private boolean canManageResources(CustomUserDetails user) {
        return user.isAdmin() || user.isManager();
    }

    private boolean canAccessResource(CustomUserDetails user, ReferralResource resource) {
        if (user.isAdmin()) return true;

        if (user.isManager()) {
            if (user.getBranchId() == null) return true;
            return resource.getOwners().stream().anyMatch(o ->
                    o.getBranchId() != null && o.getBranchId().equals(user.getBranchId()));
        }

        // Everyone else can only access resources they are assigned to
        return resource.getIsActive()
                && resource.getOwners().stream().anyMatch(o -> o.getId().equals(user.getUserId()));
    }

    private boolean canAccessOwnerResources(CustomUserDetails user, User owner) {
        if (user.isAdmin()) return true;

        if (user.isManager()) {
            if (user.getBranchId() != null && owner.getBranchId() != null) {
                return owner.getBranchId().equals(user.getBranchId());
            }
            return true;
        }

        // Everyone else can only look up resources for themselves
        return owner.getId().equals(user.getUserId());
    }

    private ReferralResourceResponse convertToResponse(ReferralResource resource) {
        ReferralResourceResponse response = new ReferralResourceResponse();
        response.setId(resource.getId());
        response.setOwnerIds(resource.getOwners().stream().map(User::getId).collect(Collectors.toList()));
        response.setOwnerNames(resource.getOwners().stream().map(User::getFullName).collect(Collectors.toList()));
        response.setOwnerType(resource.getOwnerType());
        response.setUploadedById(resource.getUploadedById());
        response.setUploadedByName(resource.getUploadedBy() != null ? resource.getUploadedBy().getFullName() : "Unknown");
        response.setStorageType(resource.getStorageType());
        response.setExternalUrl(resource.getExternalUrl());
        response.setFilePath(resource.getFilePath());
        response.setFileName(resource.getFileName());
        response.setFileSize(resource.getFileSize());
        response.setMimeType(resource.getMimeType());
        response.setResourceType(resource.getResourceType());
        response.setDescription(resource.getDescription());
        response.setIsActive(resource.getIsActive());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        response.setExpiresAt(resource.getExpiresAt());
        return response;
    }
}