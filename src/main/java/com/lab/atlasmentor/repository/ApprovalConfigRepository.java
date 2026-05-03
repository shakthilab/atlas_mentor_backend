package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ApprovalConfig;
import com.lab.atlasmentor.enums.ApprovalActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalConfigRepository extends JpaRepository<ApprovalConfig, Long> {
    
    Optional<ApprovalConfig> findByActionType(ApprovalActionType actionType);
    
    List<ApprovalConfig> findByIsActiveTrueOrderByActionType();
    
    List<ApprovalConfig> findByIsActiveFalseOrderByActionType();
    
    @Query("SELECT ac FROM ApprovalConfig ac WHERE ac.actionType = :actionType AND ac.isActive = true AND ac.isDeleted = false")
    Optional<ApprovalConfig> findActiveByActionType(@Param("actionType") ApprovalActionType actionType);
    
    @Query("SELECT ac FROM ApprovalConfig ac WHERE ac.isDeleted = false ORDER BY ac.actionType")
    List<ApprovalConfig> findAllActiveOrderByActionType();
    
    @Query("SELECT COUNT(ac) FROM ApprovalConfig ac WHERE ac.actionType = :actionType AND ac.isActive = true AND ac.isDeleted = false")
    Long countActiveByActionType(@Param("actionType") ApprovalActionType actionType);
}
