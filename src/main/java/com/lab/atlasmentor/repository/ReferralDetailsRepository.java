package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.ReferralType;
import com.lab.atlasmentor.model.ReferralDetails;
import com.lab.atlasmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralDetailsRepository extends JpaRepository<ReferralDetails, Long> {
    
    Optional<ReferralDetails> findByUser(User user);
    
    @Query("SELECT rd FROM ReferralDetails rd WHERE rd.user.id = :userId")
    Optional<ReferralDetails> findByUserId(@Param("userId") Long userId);
    
    List<ReferralDetails> findByReferralType(ReferralType referralType);
    
    @Query("SELECT rd.user.id FROM ReferralDetails rd WHERE (:referralType IS NULL OR rd.referralType = :referralType)")
    List<Long> findUserIdsByReferralType(@Param("referralType") ReferralType referralType);
}
