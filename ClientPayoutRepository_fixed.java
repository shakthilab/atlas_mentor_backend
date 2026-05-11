// This is the fixed version of the problematic methods
// Replace the existing methods in ClientPayoutRepository.java with these

    // Advanced filtering for admin role with search and multiple criteria
    @Query(value = "SELECT cp.* FROM client_payouts cp " +
           "JOIN students s ON s.id = cp.student_id " +
           "JOIN users u ON u.id = s.user_id " +
           "WHERE cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND (:search IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:source IS NULL OR cp.source_type = :source) " +
           "AND (:branch IS NULL OR s.branch_id = :branch) " +
           "AND (:paymentStatus IS NULL OR cp.payout_status = :paymentStatus) " +
           "AND (:dateFrom IS NULL OR cp.created_at >= :dateFrom) " +
           "AND (:dateTo IS NULL OR cp.created_at <= :dateTo) " +
           "ORDER BY cp.created_at DESC", nativeQuery = true)
    List<ClientPayout> findWithFiltersForAdmin(@Param("search") String search,
                                               @Param("source") String source,
                                               @Param("branch") Long branch,
                                               @Param("paymentStatus") String paymentStatus,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateTo") LocalDateTime dateTo);
    
    // Advanced filtering for manager/branch partner role with search and multiple criteria
    @Query(value = "SELECT cp.* FROM client_payouts cp " +
           "JOIN students s ON s.id = cp.student_id " +
           "JOIN users u ON u.id = s.user_id " +
           "WHERE s.branch_id = :branchId " +
           "AND cp.source_type IN ('REFERRAL', 'COMPANY') " +
           "AND (:search IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:source IS NULL OR cp.source_type = :source) " +
           "AND (:paymentStatus IS NULL OR cp.payout_status = :paymentStatus) " +
           "AND (:dateFrom IS NULL OR cp.created_at >= :dateFrom) " +
           "AND (:dateTo IS NULL OR cp.created_at <= :dateTo) " +
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
           "AND (:search IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:paymentStatus IS NULL OR cp.payout_status = :paymentStatus) " +
           "AND (:dateFrom IS NULL OR cp.created_at >= :dateFrom) " +
           "AND (:dateTo IS NULL OR cp.created_at <= :dateTo) " +
           "ORDER BY cp.created_at DESC", nativeQuery = true)
    List<ClientPayout> findWithFiltersForUser(@Param("userId") Long userId,
                                             @Param("sourceType") String sourceType,
                                             @Param("search") String search,
                                             @Param("paymentStatus") String paymentStatus,
                                             @Param("dateFrom") LocalDateTime dateFrom,
                                             @Param("dateTo") LocalDateTime dateTo);
