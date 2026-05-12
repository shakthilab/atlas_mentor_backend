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

    @Query(value = "SELECT rd.referral_type, COUNT(*) FROM referral_details rd GROUP BY rd.referral_type", nativeQuery = true)
    List<Object[]> countGroupByReferralType();

    @Query(value = "SELECT u.status, COUNT(*) FROM referral_details rd JOIN users u ON rd.user_id = u.id GROUP BY u.status", nativeQuery = true)
    List<Object[]> countGroupByUserStatus();

    @Query(value = "SELECT rd.referral_type, COUNT(*) FROM referral_details rd " +
           "JOIN client_payouts cp ON cp.user_id = rd.user_id " +
           "JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId GROUP BY rd.referral_type", nativeQuery = true)
    List<Object[]> countGroupByReferralTypeForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT u.status, COUNT(*) FROM referral_details rd " +
           "JOIN users u ON rd.user_id = u.id " +
           "JOIN client_payouts cp ON cp.user_id = rd.user_id " +
           "JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId GROUP BY u.status", nativeQuery = true)
    List<Object[]> countGroupByUserStatusForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT rd.referral_type, u.status FROM referral_details rd " +
           "JOIN users u ON rd.user_id = u.id WHERE rd.user_id = :userId", nativeQuery = true)
    List<Object[]> findTypeAndStatusByUserId(@Param("userId") Long userId);
}
