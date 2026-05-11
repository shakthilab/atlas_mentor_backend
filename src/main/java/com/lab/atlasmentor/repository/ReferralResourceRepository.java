package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ReferralResource;
import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReferralResourceRepository extends JpaRepository<ReferralResource, Long> {
    
    List<ReferralResource> findByOwner_Id(Long ownerId);
    
    List<ReferralResource> findByOwner_IdAndIsActiveTrue(Long ownerId);
    
    List<ReferralResource> findByOwner_IdAndOwnerType(Long ownerId, OwnerType ownerType);
    
    List<ReferralResource> findByOwner_IdAndOwnerTypeAndIsActiveTrue(Long ownerId, OwnerType ownerType);
    
    List<ReferralResource> findByUploadedBy_Id(Long uploadedById);
    
    @Query("SELECT rr FROM ReferralResource rr WHERE rr.owner.id = :ownerId AND rr.isActive = true ORDER BY rr.createdAt DESC")
    List<ReferralResource> findActiveResourcesByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT rr FROM ReferralResource rr WHERE rr.expiresAt IS NOT NULL AND rr.expiresAt < :now AND rr.isActive = true")
    List<ReferralResource> findExpiredResources(@Param("now") LocalDateTime now);
    
    @Query("SELECT rr FROM ReferralResource rr WHERE rr.owner.id IN :ownerIds AND rr.isActive = true")
    List<ReferralResource> findByOwnerIds(@Param("ownerIds") List<Long> ownerIds);
    
    @Query("SELECT rr FROM ReferralResource rr WHERE rr.ownerType = :ownerType AND rr.isActive = true")
    List<ReferralResource> findByOwnerType(@Param("ownerType") OwnerType ownerType);
    
    long countByOwner_Id(Long ownerId);
    
    long countByOwner_IdAndIsActiveTrue(Long ownerId);
    
    long countByOwnerType(OwnerType ownerType);
}
