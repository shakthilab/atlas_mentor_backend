package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ReferralAssignment;
import com.lab.atlasmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralAssignmentRepository extends JpaRepository<ReferralAssignment, Long> {
    
    List<ReferralAssignment> findByReferralId(Long referralId);
    
    List<ReferralAssignment> findByAssignedToId(Long assignedToId);
    
    Optional<ReferralAssignment> findByReferralIdAndAssignedToId(Long referralId, Long assignedToId);
    
    @Query("SELECT ra.assignedTo FROM ReferralAssignment ra WHERE ra.referral.id = :referralId")
    List<User> findAssignedUsersByReferralId(@Param("referralId") Long referralId);
    
    @Query("SELECT ra.referral FROM ReferralAssignment ra WHERE ra.assignedTo.id = :assignedToId")
    List<User> findReferralsByAssignedToId(@Param("assignedToId") Long assignedToId);
    
    void deleteByReferralIdAndAssignedToId(Long referralId, Long assignedToId);
    
    void deleteByReferralId(Long referralId);
}
