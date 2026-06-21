package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ReferralResource;
import com.lab.atlasmentor.model.ReferralResource.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReferralResourceRepository extends JpaRepository<ReferralResource, Long> {

    @Query("SELECT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId")
    List<ReferralResource> findByOwner_Id(@Param("ownerId") Long ownerId);

    @Query("SELECT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId AND rr.isActive = true")
    List<ReferralResource> findByOwner_IdAndIsActiveTrue(@Param("ownerId") Long ownerId);

    @Query("SELECT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId AND rr.ownerType = :ownerType")
    List<ReferralResource> findByOwner_IdAndOwnerType(@Param("ownerId") Long ownerId, @Param("ownerType") OwnerType ownerType);

    @Query("SELECT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId AND rr.ownerType = :ownerType AND rr.isActive = true")
    List<ReferralResource> findByOwner_IdAndOwnerTypeAndIsActiveTrue(@Param("ownerId") Long ownerId, @Param("ownerType") OwnerType ownerType);

    @Query("SELECT rr FROM ReferralResource rr WHERE rr.uploadedBy.id = :uploadedById")
    List<ReferralResource> findByUploadedBy_Id(@Param("uploadedById") Long uploadedById);

    @Query("SELECT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId AND rr.isActive = true ORDER BY rr.createdAt DESC")
    List<ReferralResource> findActiveResourcesByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT rr FROM ReferralResource rr WHERE rr.expiresAt IS NOT NULL AND rr.expiresAt < :now AND rr.isActive = true")
    List<ReferralResource> findExpiredResources(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT rr FROM ReferralResource rr JOIN rr.owners o WHERE o.id IN :ownerIds AND rr.isActive = true")
    List<ReferralResource> findByOwnerIds(@Param("ownerIds") List<Long> ownerIds);

    @Query("SELECT rr FROM ReferralResource rr WHERE rr.ownerType = :ownerType AND rr.isActive = true")
    List<ReferralResource> findByOwnerType(@Param("ownerType") OwnerType ownerType);

    @Query("SELECT COUNT(rr) FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId")
    long countByOwner_Id(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(rr) FROM ReferralResource rr JOIN rr.owners o WHERE o.id = :ownerId AND rr.isActive = true")
    long countByOwner_IdAndIsActiveTrue(@Param("ownerId") Long ownerId);

    long countByOwnerType(OwnerType ownerType);

    @Modifying
    @Query(value = "DELETE FROM referral_resource_owners WHERE owner_id = :userId", nativeQuery = true)
    void deleteOwnerByUserId(@Param("userId") Long userId);
}