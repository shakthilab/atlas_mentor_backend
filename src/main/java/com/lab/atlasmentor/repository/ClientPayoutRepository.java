package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.SourceType;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientPayoutRepository extends JpaRepository<ClientPayout, Long> {
    
    // Find by user and status
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.payoutStatus = :status ORDER BY cp.createdAt DESC")
    List<ClientPayout> findByUserIdAndPayoutStatusOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("status") ClientPayoutStatus status);
    
    // Find all payouts ordered by creation date
    @Query("SELECT cp FROM ClientPayout cp ORDER BY cp.createdAt DESC")
    List<ClientPayout> findAllByOrderByCreatedAtDesc();
    
    // Find by user (all statuses)
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.user.id = :userId ORDER BY cp.createdAt DESC")
    List<ClientPayout> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // Find by student
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.student.id = :studentId")
    Optional<ClientPayout> findByStudentId(@Param("studentId") Long studentId);
    
    // Find by student and source type
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.student.id = :studentId AND cp.sourceType = :sourceType")
    List<ClientPayout> findByStudentIdAndSourceType(@Param("studentId") Long studentId, @Param("sourceType") SourceType sourceType);
    
    // Find by source type and user ID
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.sourceType = :sourceType")
    List<ClientPayout> findByUserIdAndSourceType(@Param("userId") Long userId, @Param("sourceType") SourceType sourceType);
    
    // Find by status
    List<ClientPayout> findByPayoutStatusOrderByCreatedAtDesc(ClientPayoutStatus status);
    
    // Find by multiple statuses
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.payoutStatus IN :statuses ORDER BY cp.createdAt DESC")
    List<ClientPayout> findByPayoutStatusInOrderByCreatedAtDesc(@Param("statuses") List<ClientPayoutStatus> statuses);
    
    // Find by date range
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.createdAt BETWEEN :startDate AND :endDate ORDER BY cp.createdAt DESC")
    List<ClientPayout> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("startDate") LocalDateTime startDate, 
                                                              @Param("endDate") LocalDateTime endDate);
    
    // Find by user and date range
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.createdAt BETWEEN :startDate AND :endDate ORDER BY cp.createdAt DESC")
    List<ClientPayout> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                      @Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate);
    
    // Find disputed payouts
    List<ClientPayout> findByPayoutStatusOrderByDisputedAtDesc(ClientPayoutStatus status);
    
    // Count by status
    @Query("SELECT COUNT(cp) FROM ClientPayout cp WHERE cp.payoutStatus = :status")
    Long countByPayoutStatus(@Param("status") ClientPayoutStatus status);
    
    // Count by user and status
    @Query("SELECT COUNT(cp) FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.payoutStatus = :status")
    Long countByUserIdAndPayoutStatus(@Param("userId") Long userId, @Param("status") ClientPayoutStatus status);
    
    // Sum assigned amounts by user
    @Query("SELECT COALESCE(SUM(cp.assignedAmount), 0) FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.payoutStatus IN :statuses")
    Double sumAssignedAmountByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<ClientPayoutStatus> statuses);
    
    // Sum paid amounts by user
    @Query("SELECT COALESCE(SUM(cp.paidAmount), 0) FROM ClientPayout cp WHERE cp.user.id = :userId AND cp.payoutStatus IN :statuses")
    Double sumPaidAmountByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<ClientPayoutStatus> statuses);
    
    // Find payouts requiring attention (disputed, pending assignment)
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.payoutStatus IN :statuses ORDER BY cp.createdAt ASC")
    List<ClientPayout> findPayoutsRequiringAttention(@Param("statuses") List<ClientPayoutStatus> statuses);
    
    // Find payouts by assigned by user
    List<ClientPayout> findByAssignedByIdOrderByCreatedAtDesc(Long assignedById);
    
    // Find payouts by disputed by user
    List<ClientPayout> findByDisputedByIdOrderByDisputedAtDesc(Long disputedById);
    
    // Find payouts for reporting
    @Query("SELECT cp FROM ClientPayout cp WHERE " +
           "(:userId IS NULL OR cp.user.id = :userId) AND " +
           "(:sourceType IS NULL OR cp.sourceType = :sourceType) AND " +
           "(:status IS NULL OR cp.payoutStatus = :status) AND " +
           "(:startDate IS NULL OR cp.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR cp.createdAt <= :endDate) " +
           "ORDER BY cp.createdAt DESC")
    List<ClientPayout> findPayoutsForReporting(@Param("userId") Long userId,
                                             @Param("sourceType") SourceType sourceType,
                                             @Param("status") ClientPayoutStatus status,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    // Check if payout exists for student
    boolean existsByStudentId(Long studentId);
    
    // Find active payouts (not deleted)
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.student.id = :studentId AND cp.payoutStatus != 'ACCEPTED'")
    Optional<ClientPayout> findActiveByStudentId(@Param("studentId") Long studentId);
    
    // Find payouts by branch (for MANAGER/BRANCH_PARTNER roles)
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.student.branch.id = :branchId AND cp.sourceType IN ('REFERRAL', 'COMPANY')")
    List<ClientPayout> findByBranchIdAndSourceTypeIn(@Param("branchId") Long branchId, @Param("sourceTypes") List<SourceType> sourceTypes);
    
    // Find all payouts with referral and company source types (for ADMIN role)
    @Query("SELECT cp FROM ClientPayout cp WHERE cp.sourceType IN ('REFERRAL', 'COMPANY')")
    List<ClientPayout> findBySourceTypeIn(@Param("sourceTypes") List<SourceType> sourceTypes);
    
    // Advanced filtering for admin role with search and multiple criteria



    @Query(value = "SELECT cp.* FROM client_payouts cp " +
           "JOIN students s ON s.id = cp.student_id " +
           "JOIN users u ON u.id = s.user_id " +
           "WHERE cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND (CAST(:search AS TEXT) IS NULL OR LOWER(CONCAT(u.first_name, u.last_name, u.email, u.phone)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))) " +
           "AND (CAST(:source AS TEXT) IS NULL OR cp.source_type = CAST(:source AS TEXT)) " +
           "AND (CAST(:branch AS BIGINT) IS NULL OR s.branch_id = CAST(:branch AS BIGINT)) " +
           "AND (CAST(:paymentStatus AS TEXT) IS NULL OR cp.payout_status = CAST(:paymentStatus AS TEXT)) " +
           "AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR cp.created_at >= CAST(:dateFrom AS TIMESTAMP)) " +
           "AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR cp.created_at <= CAST(:dateTo AS TIMESTAMP)) " +
           "ORDER BY cp.created_at DESC", nativeQuery = true)
    List<ClientPayout> findWithFiltersForAdmin(@Param("search") String search,
                                               @Param("source") String source,
                                               @Param("b" +
                                                       "ranch") Long branch,
                                               @Param("paymentStatus") String paymentStatus,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateTo") LocalDateTime dateTo);
    
    // Advanced filtering for manager/branch partner role with search and multiple criteria
    @Query(value = "SELECT cp.* FROM client_payouts cp " +
           "JOIN students s ON s.id = cp.student_id " +
           "JOIN users u ON u.id = s.user_id " +
           "WHERE s.branch_id = :branchId " +
           "AND cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND (CAST(:search AS TEXT) IS NULL OR LOWER(CONCAT(u.first_name, u.last_name, u.email, u.phone)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))) " +
           "AND (CAST(:source AS TEXT) IS NULL OR cp.source_type = CAST(:source AS TEXT)) " +
           "AND (CAST(:paymentStatus AS TEXT) IS NULL OR cp.payout_status = CAST(:paymentStatus AS TEXT)) " +
           "AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR cp.created_at >= CAST(:dateFrom AS TIMESTAMP)) " +
           "AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR cp.created_at <= CAST(:dateTo AS TIMESTAMP)) " +
           "ORDER BY cp.created_at DESC", nativeQuery = true)
    List<ClientPayout> findWithFiltersForBranch(@Param("branchId") Long branchId,
                                               @Param("search") String search,
                                               @Param("source") String source,
                                               @Param("paymentStatus") String paymentStatus,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateTo") LocalDateTime dateTo);
    
    // Advanced filtering for referral/company role with search and multiple criteria
    @Query(value = "SELECT cp.* FROM client_payouts cp " +
           "JOIN students s ON s.id = cp.student_id " +
           "JOIN users u ON u.id = s.user_id " +
           "WHERE cp.user_id = :userId " +
           "AND cp.source_type = :sourceType " +
           "AND (CAST(:search AS TEXT) IS NULL OR LOWER(CONCAT(u.first_name, u.last_name, u.email, u.phone)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))) " +
           "AND (CAST(:paymentStatus AS TEXT) IS NULL OR cp.payout_status = CAST(:paymentStatus AS TEXT)) " +
           "AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR cp.created_at >= CAST(:dateFrom AS TIMESTAMP)) " +
           "AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR cp.created_at <= CAST(:dateTo AS TIMESTAMP)) " +
           "ORDER BY cp.created_at DESC", nativeQuery = true)
    List<ClientPayout> findWithFiltersForUser(@Param("userId") Long userId,
                                             @Param("sourceType") String sourceType,
                                             @Param("search") String search,
                                             @Param("paymentStatus") String paymentStatus,
                                             @Param("dateFrom") LocalDateTime dateFrom,
                                             @Param("dateTo") LocalDateTime dateTo);
}
