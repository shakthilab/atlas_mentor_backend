package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.SourceType;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Dashboard aggregate queries - global (ADMIN)
    @Query(value = "SELECT cp.payout_status, COUNT(*), " +
           "COALESCE(SUM(cp.assigned_amount), 0), COALESCE(SUM(cp.paid_amount), 0), COALESCE(SUM(cp.dispute_amount), 0) " +
           "FROM client_payouts cp WHERE cp.source_type IN ('REFERRAL', 'COMPANY') GROUP BY cp.payout_status", nativeQuery = true)
    List<Object[]> getPayoutStatsByStatusGlobal();

    // Dashboard aggregate queries - branch-scoped (MANAGER/BRANCH_PARTNER)
    @Query(value = "SELECT cp.payout_status, COUNT(*), " +
           "COALESCE(SUM(cp.assigned_amount), 0), COALESCE(SUM(cp.paid_amount), 0), COALESCE(SUM(cp.dispute_amount), 0) " +
           "FROM client_payouts cp JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.source_type IN ('REFERRAL', 'COMPANY') GROUP BY cp.payout_status", nativeQuery = true)
    List<Object[]> getPayoutStatsByStatusForBranch(@Param("branchId") Long branchId);

    // Dashboard aggregate queries - user-scoped (REFERRAL/COMPANY)
    @Query(value = "SELECT cp.payout_status, COUNT(*), " +
           "COALESCE(SUM(cp.assigned_amount), 0), COALESCE(SUM(cp.paid_amount), 0), COALESCE(SUM(cp.dispute_amount), 0) " +
           "FROM client_payouts cp WHERE cp.user_id = :userId GROUP BY cp.payout_status", nativeQuery = true)
    List<Object[]> getPayoutStatsByStatusForUser(@Param("userId") Long userId);

    // Commission trend - daily data - global (ADMIN)
    @Query(value = "SELECT DATE(cp.created_at) as day, " +
           "COALESCE(SUM(cp.paid_amount), 0) as commission_received, " +
           "COALESCE(SUM(CASE WHEN cp.assigned_amount IS NOT NULL THEN cp.assigned_amount - COALESCE(cp.paid_amount, 0) ELSE 0 END), 0) as pending_balance " +
           "FROM client_payouts cp " +
           "WHERE cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND DATE(cp.created_at) BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE) " +
           "GROUP BY DATE(cp.created_at) ORDER BY DATE(cp.created_at)", nativeQuery = true)
    List<Object[]> getTrendDataGlobal(@Param("fromDate") java.time.LocalDate fromDate,
                                      @Param("toDate") java.time.LocalDate toDate);

    // Commission trend - daily data - branch-scoped (MANAGER/BRANCH_PARTNER)
    @Query(value = "SELECT DATE(cp.created_at) as day, " +
           "COALESCE(SUM(cp.paid_amount), 0) as commission_received, " +
           "COALESCE(SUM(CASE WHEN cp.assigned_amount IS NOT NULL THEN cp.assigned_amount - COALESCE(cp.paid_amount, 0) ELSE 0 END), 0) as pending_balance " +
           "FROM client_payouts cp JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND DATE(cp.created_at) BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE) " +
           "GROUP BY DATE(cp.created_at) ORDER BY DATE(cp.created_at)", nativeQuery = true)
    List<Object[]> getTrendDataForBranch(@Param("branchId") Long branchId,
                                         @Param("fromDate") java.time.LocalDate fromDate,
                                         @Param("toDate") java.time.LocalDate toDate);

    // Commission trend - daily data - user-scoped (REFERRAL/COMPANY)
    @Query(value = "SELECT DATE(cp.created_at) as day, " +
           "COALESCE(SUM(cp.paid_amount), 0) as commission_received, " +
           "COALESCE(SUM(CASE WHEN cp.assigned_amount IS NOT NULL THEN cp.assigned_amount - COALESCE(cp.paid_amount, 0) ELSE 0 END), 0) as pending_balance " +
           "FROM client_payouts cp " +
           "WHERE cp.user_id = :userId " +
           "AND DATE(cp.created_at) BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE) " +
           "GROUP BY DATE(cp.created_at) ORDER BY DATE(cp.created_at)", nativeQuery = true)
    List<Object[]> getTrendDataForUser(@Param("userId") Long userId,
                                       @Param("fromDate") java.time.LocalDate fromDate,
                                       @Param("toDate") java.time.LocalDate toDate);
    
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

    @Modifying
    @Query("DELETE FROM ClientPayout cp WHERE cp.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM ClientPayout cp WHERE cp.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ClientPayout cp SET cp.disputedBy = null WHERE cp.disputedBy.id = :userId")
    void nullifyDisputedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ClientPayout cp SET cp.respondedBy = null WHERE cp.respondedBy.id = :userId")
    void nullifyRespondedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ClientPayout cp SET cp.assignedBy = null WHERE cp.assignedBy.id = :userId")
    void nullifyAssignedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ClientPayout cp SET cp.lastPaidBy = null WHERE cp.lastPaidBy.id = :userId")
    void nullifyLastPaidByUserId(@Param("userId") Long userId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT COALESCE(SUM(COALESCE(assigned_amount, 0) - COALESCE(paid_amount, 0)), 0) " +
           "FROM client_payouts WHERE payout_status NOT IN ('PAID','ACCEPTED','REJECTED') AND assigned_amount IS NOT NULL AND created_at >= :from", nativeQuery = true)
    java.math.BigDecimal getTotalPendingPayouts(@Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(COALESCE(dispute_amount, assigned_amount, 0)), 0) " +
           "FROM client_payouts WHERE payout_status = 'DISPUTE' AND created_at >= :from", nativeQuery = true)
    List<Object[]> getDisputeSummary(@Param("from") LocalDateTime from);

    @Query(value = "SELECT CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as entity_name, cp.source_type, " +
           "COALESCE(SUM(cp.assigned_amount), 0) as total_amount, cp.payout_status, " +
           "MAX(COALESCE(cp.last_paid_at, cp.assigned_at, cp.created_at)) as last_activity " +
           "FROM client_payouts cp JOIN users u ON cp.user_id = u.id " +
           "WHERE cp.payout_status NOT IN ('ACCEPTED','REJECTED') AND cp.created_at >= :from " +
           "GROUP BY u.id, u.first_name, u.last_name, cp.source_type, cp.payout_status " +
           "ORDER BY total_amount DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTopReceivables(@Param("from") LocalDateTime from);

    @Query(value = "SELECT cp.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as dispute_name, " +
           "cp.dispute_reason, COALESCE(cp.dispute_amount, cp.assigned_amount, 0) as amount, cp.disputed_at " +
           "FROM client_payouts cp JOIN users u ON cp.user_id = u.id " +
           "WHERE cp.payout_status = 'DISPUTE' AND cp.created_at >= :from ORDER BY cp.disputed_at DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findOpenDisputes(@Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', cp.created_at), 'Mon') as month, " +
           "COALESCE(rd.referral_type, cp.source_type) as partner_type, " +
           "COALESCE(SUM(cp.paid_amount), 0) as earnings " +
           "FROM client_payouts cp " +
           "LEFT JOIN referral_details rd ON rd.user_id = cp.user_id " +
           "WHERE cp.created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', cp.created_at), COALESCE(rd.referral_type, cp.source_type) " +
           "ORDER BY DATE_TRUNC('month', cp.created_at)", nativeQuery = true)
    List<Object[]> getMonthlyEarningsByPartnerType(@Param("from") LocalDateTime from);

    @Query(value = "SELECT " +
           "COUNT(DISTINCT cp.user_id) as referred_count, " +
           "COUNT(DISTINCT CASE WHEN s.status IN ('LEAD','PROSPECTIVE','REGISTERED','STUDENT') THEN s.id END) as lead_count, " +
           "COUNT(DISTINCT CASE WHEN s.status IN ('REGISTERED','STUDENT') THEN s.id END) as registered_count, " +
           "COUNT(DISTINCT CASE WHEN s.status = 'STUDENT' THEN s.id END) as active_student_count, " +
           "COUNT(DISTINCT CASE WHEN sp.payment_status = 'PAID' THEN sp.id END) as paid_count " +
           "FROM client_payouts cp " +
           "JOIN students s ON cp.student_id = s.id " +
           "LEFT JOIN student_payments sp ON sp.student_id = s.id AND sp.is_deleted = false " +
           "WHERE cp.created_at >= :from", nativeQuery = true)
    List<Object[]> getReferralFunnel(@Param("from") LocalDateTime from);

    @Query(value = "SELECT CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as referrer_name, " +
           "COALESCE(rd.referral_type, cp.source_type) as partner_type, " +
           "COUNT(DISTINCT cp.student_id) as students_count, " +
           "COALESCE(SUM(cp.paid_amount), 0) as commission_amount " +
           "FROM client_payouts cp " +
           "JOIN users u ON cp.user_id = u.id " +
           "LEFT JOIN referral_details rd ON rd.user_id = u.id " +
           "WHERE cp.created_at >= :from " +
           "GROUP BY u.id, u.first_name, u.last_name, rd.referral_type, cp.source_type " +
           "ORDER BY commission_amount DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopReferrers(@Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM client_payouts WHERE payout_status IN ('PENDING','AMOUNT_ASSIGNED')", nativeQuery = true)
    Long countPayoutsAwaitingApproval();

    // ==================== BRANCH-SCOPED DASHBOARD QUERIES ====================

    @Query(value = "SELECT COALESCE(SUM(COALESCE(cp.assigned_amount, 0) - COALESCE(cp.paid_amount, 0)), 0) " +
           "FROM client_payouts cp JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.payout_status NOT IN ('PAID','ACCEPTED','REJECTED') AND cp.assigned_amount IS NOT NULL AND cp.created_at >= :from", nativeQuery = true)
    java.math.BigDecimal getTotalPendingPayoutsForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(COALESCE(cp.dispute_amount, cp.assigned_amount, 0)), 0) " +
           "FROM client_payouts cp JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.payout_status = 'DISPUTE' AND cp.created_at >= :from", nativeQuery = true)
    List<Object[]> getDisputeSummaryForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as entity_name, cp.source_type, " +
           "COALESCE(SUM(cp.assigned_amount), 0) as total_amount, cp.payout_status, " +
           "MAX(COALESCE(cp.last_paid_at, cp.assigned_at, cp.created_at)) as last_activity " +
           "FROM client_payouts cp JOIN users u ON cp.user_id = u.id JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.payout_status NOT IN ('ACCEPTED','REJECTED') AND cp.created_at >= :from " +
           "GROUP BY u.id, u.first_name, u.last_name, cp.source_type, cp.payout_status " +
           "ORDER BY total_amount DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTopReceivablesForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT cp.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as dispute_name, " +
           "cp.dispute_reason, COALESCE(cp.dispute_amount, cp.assigned_amount, 0) as amount, cp.disputed_at " +
           "FROM client_payouts cp JOIN users u ON cp.user_id = u.id JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.payout_status = 'DISPUTE' AND cp.created_at >= :from ORDER BY cp.disputed_at DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findOpenDisputesForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', cp.created_at), 'Mon') as month, " +
           "COALESCE(rd.referral_type, cp.source_type) as partner_type, " +
           "COALESCE(SUM(cp.paid_amount), 0) as earnings " +
           "FROM client_payouts cp " +
           "LEFT JOIN referral_details rd ON rd.user_id = cp.user_id " +
           "JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', cp.created_at), COALESCE(rd.referral_type, cp.source_type) " +
           "ORDER BY DATE_TRUNC('month', cp.created_at)", nativeQuery = true)
    List<Object[]> getMonthlyEarningsByPartnerTypeForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT " +
           "COUNT(DISTINCT cp.user_id) as referred_count, " +
           "COUNT(DISTINCT CASE WHEN s.status IN ('LEAD','PROSPECTIVE','REGISTERED','STUDENT') THEN s.id END) as lead_count, " +
           "COUNT(DISTINCT CASE WHEN s.status IN ('REGISTERED','STUDENT') THEN s.id END) as registered_count, " +
           "COUNT(DISTINCT CASE WHEN s.status = 'STUDENT' THEN s.id END) as active_student_count, " +
           "COUNT(DISTINCT CASE WHEN sp.payment_status = 'PAID' THEN sp.id END) as paid_count " +
           "FROM client_payouts cp " +
           "JOIN students s ON cp.student_id = s.id " +
           "LEFT JOIN student_payments sp ON sp.student_id = s.id AND sp.is_deleted = false " +
           "WHERE s.branch_id = :branchId AND cp.created_at >= :from", nativeQuery = true)
    List<Object[]> getReferralFunnelForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as referrer_name, " +
           "COALESCE(rd.referral_type, cp.source_type) as partner_type, " +
           "COUNT(DISTINCT cp.student_id) as students_count, " +
           "COALESCE(SUM(cp.paid_amount), 0) as commission_amount " +
           "FROM client_payouts cp " +
           "JOIN users u ON cp.user_id = u.id " +
           "JOIN students s ON cp.student_id = s.id " +
           "LEFT JOIN referral_details rd ON rd.user_id = u.id " +
           "WHERE s.branch_id = :branchId AND cp.created_at >= :from " +
           "GROUP BY u.id, u.first_name, u.last_name, rd.referral_type, cp.source_type " +
           "ORDER BY commission_amount DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopReferrersForBranch(@Param("branchId") Long branchId, @Param("from") LocalDateTime from);

    @Query(value = "SELECT COUNT(*) FROM client_payouts cp JOIN students s ON cp.student_id = s.id " +
           "WHERE s.branch_id = :branchId AND cp.payout_status IN ('PENDING','AMOUNT_ASSIGNED')", nativeQuery = true)
    Long countPayoutsAwaitingApprovalForBranch(@Param("branchId") Long branchId);

    // ==================== PARTNER (USER-SCOPED) DASHBOARD QUERIES ====================

    @Query(value = "SELECT COUNT(DISTINCT cp.student_id) FROM client_payouts cp " +
           "WHERE cp.user_id = :userId AND cp.created_at BETWEEN :from AND :to", nativeQuery = true)
    Long countStudentsByUserIdBetween(@Param("userId") Long userId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query(value = "SELECT COALESCE(SUM(cp.assigned_amount), 0), COALESCE(SUM(cp.paid_amount), 0) " +
           "FROM client_payouts cp WHERE cp.user_id = :userId AND cp.created_at >= :from", nativeQuery = true)
    List<Object[]> getAmountSummaryForUser(@Param("userId") Long userId,
                                           @Param("from") LocalDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', cp.created_at), 'Mon') as month, " +
           "COALESCE(SUM(cp.assigned_amount), 0) as assigned, " +
           "COALESCE(SUM(cp.paid_amount), 0) as paid " +
           "FROM client_payouts cp " +
           "WHERE cp.user_id = :userId AND cp.created_at >= :from " +
           "GROUP BY DATE_TRUNC('month', cp.created_at) " +
           "ORDER BY DATE_TRUNC('month', cp.created_at)", nativeQuery = true)
    List<Object[]> getMonthlyEarningsForUser(@Param("userId") Long userId,
                                              @Param("from") LocalDateTime from);
}
