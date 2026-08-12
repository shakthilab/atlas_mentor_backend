package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByVerificationToken(String verificationToken);
    
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    
    @Modifying
    @Query("UPDATE User u SET u.isVerified = true, u.verificationToken = null, u.verificationTokenExpiresAt = null WHERE u.id = :userId")
    void verifyUser(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phone = :phone")
    boolean existsByPhone(@Param("phone") String phone);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phone = :phone AND u.id != :excludeUserId")
    boolean existsByPhoneExcludingUser(@Param("phone") String phone, @Param("excludeUserId") Long excludeUserId);
    
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.passwordResetToken = null, u.passwordResetTokenExpiresAt = null WHERE u.id = :userId")
    void resetPassword(@Param("userId") Long userId, @Param("password") String password);
    
    @Query("SELECT DISTINCT u FROM User u WHERE u.role.name IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);
    
    @Query(value = "SELECT DISTINCT u.* FROM users u " +
           "JOIN roles r ON r.id = u.role_id " +
           "WHERE (:role IS NULL OR r.name = :role) " +
           "AND (:branch IS NULL OR u.branch_id = :branch) " +
           "AND (:search IS NULL OR " +
           "CAST(u.first_name AS TEXT) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(u.last_name AS TEXT) LIKE CONCAT('%', :search, '%') OR " +
           "CONCAT(u.first_name, ' ', u.last_name) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(u.email AS TEXT) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(r.name AS TEXT) LIKE CONCAT('%', :search, '%')) " +
           "AND r.name IN :employeeRoleNames", 
           nativeQuery = true)
    Page<User> findEmployeesWithFilters(@Param("role") String role,
                                       @Param("branch") Long branch,
                                       @Param("search") String search,
                                       @Param("employeeRoleNames") List<String> employeeRoleNames,
                                       Pageable pageable);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") UserStatus status);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void updateFailedAttempts(@Param("userId") Long userId, @Param("attempts") int attempts, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :userId")
    void resetFailedAttempts(@Param("userId") Long userId);
    
    // Hierarchy query methods
    @Query("SELECT DISTINCT u FROM User u WHERE u.role.name = 'MANAGER'")
    List<User> findAllManagers();
    
    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findUsersByIds(@Param("userIds") List<Long> userIds);
    
    @Query("SELECT u FROM User u WHERE u.role.name IN ('SENIOR_COUNSELLOR', 'JUNIOR_COUNSELLOR', 'COUNSELLOR') AND u.id IN :userIds")
    List<User> findCounsellorsByIds(@Param("userIds") List<Long> userIds);
    
    @Query("SELECT u FROM User u WHERE u.role.name = 'SENIOR_COUNSELLOR'")
    List<User> findAllSeniorCounsellors();
    
    @Query("SELECT u FROM User u WHERE u.role.name IN ('JUNIOR_COUNSELLOR', 'COUNSELLOR')")
    List<User> findAllJuniorCounsellors();
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.assignedBy.id = :counsellorId")
    Long countStudentsByCounsellorId(@Param("counsellorId") Long counsellorId);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles AND (:roleId IS NULL OR u.role.id = :roleId) AND (:branchId IS NULL OR u.branch.id = :branchId)")
    List<User> findUsersExcludingRolesWithRoleIdAndBranchId(@Param("excludedRoles") List<String> excludedRoles, @Param("roleId") Long roleId, @Param("branchId") Long branchId);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles AND (:roleId IS NULL OR u.role.id = :roleId)")
    List<User> findUsersExcludingRolesWithRoleId(@Param("excludedRoles") List<String> excludedRoles, @Param("roleId") Long roleId);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles AND (:roleIds IS NULL OR u.role.id IN :roleIds) AND (:branchId IS NULL OR u.branch.id = :branchId)")
    List<User> findUsersExcludingRolesWithRoleIdsAndBranchId(@Param("excludedRoles") List<String> excludedRoles, @Param("roleIds") List<Long> roleIds, @Param("branchId") Long branchId);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles AND (:roleIds IS NULL OR u.role.id IN :roleIds)")
    List<User> findUsersExcludingRolesWithRoleIds(@Param("excludedRoles") List<String> excludedRoles, @Param("roleIds") List<Long> roleIds);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles AND (:branchId IS NULL OR u.branch.id = :branchId)")
    List<User> findUsersExcludingRolesWithBranchId(@Param("excludedRoles") List<String> excludedRoles, @Param("branchId") Long branchId);
    
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN :excludedRoles")
    List<User> findUsersExcludingRoles(@Param("excludedRoles") List<String> excludedRoles);
    
    // Count methods for staff and students
    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId AND u.role.name IN :staffRoles")
    Long countStaffsByBranchId(@Param("branchId") Long branchId, @Param("staffRoles") List<String> staffRoles);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId AND u.role.name = 'STUDENT'")
    Long countStudentsByBranchId(@Param("branchId") Long branchId);
    
    // Branch-based access control methods
    @Query(value = "SELECT u.* FROM users u WHERE (:isAdmin = true OR u.branch_id = :branchId)", nativeQuery = true)
    List<User> findAllWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT u.* FROM users u WHERE (:isAdmin = true OR u.branch_id = :branchId)", nativeQuery = true)
    List<User> findAllActiveWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT u.* FROM users u WHERE (:isAdmin = true OR u.branch_id = :branchId) AND u.role_id IN (SELECT r.id FROM roles r WHERE r.name IN :roleNames)", nativeQuery = true)
    List<User> findByRoleNamesWithAccess(@Param("roleNames") List<String> roleNames, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    // Hierarchy-based queries
    @Query("SELECT u FROM User u WHERE u.reportingManager.id = :seniorId AND u.role.name = 'JUNIOR_COUNSELLOR' AND u.branch.id = :branchId")
    List<User> findJuniorCounsellorsBySeniorIdAndBranchId(@Param("seniorId") Long seniorId, @Param("branchId") Long branchId);
    
    @Query("SELECT u.id FROM User u WHERE u.reportingManager.id = :seniorId AND u.role.name = 'JUNIOR_COUNSELLOR' AND u.branch.id = :branchId")
    List<Long> findJuniorCounsellorIdsBySeniorIdAndBranchId(@Param("seniorId") Long seniorId, @Param("branchId") Long branchId);
    
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    Optional<User> findFirstByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM User u WHERE u.role.name IN ('SENIOR_COUNSELLOR', 'JUNIOR_COUNSELLOR', 'COUNSELLOR') AND u.branch.id = :branchId AND u.status = 'ACTIVE'")
    List<User> findActiveCounsellorsByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT u FROM User u WHERE u.role.name IN ('SENIOR_COUNSELLOR', 'JUNIOR_COUNSELLOR') AND u.branch.id = :branchId")
    List<User> findSeniorAndJuniorCounsellorsByBranchId(@Param("branchId") Long branchId);
    
    // Task bundle related queries
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId AND u.status = :status")
    List<User> findByRoleIdAndStatus(@Param("roleId") Long roleId, @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role.id IN :roleIds AND u.branch.id = :branchId AND u.status = 'ACTIVE'")
    List<User> findActiveUsersByRoleIdsAndBranchId(@Param("roleIds") List<Long> roleIds, @Param("branchId") Long branchId);

    @Query("SELECT u FROM User u WHERE (:roleId IS NULL OR u.role.id = :roleId) AND (:branchId IS NULL OR u.branch.id = :branchId) AND u.status = 'ACTIVE'")
    List<User> findActiveUsersByRoleIdAndBranchId(@Param("roleId") Long roleId, @Param("branchId") Long branchId);

    @Modifying
    @Query("UPDATE User u SET u.reportingManager = null WHERE u.reportingManager.id = :userId")
    void nullifyReportingManagerByUserId(@Param("userId") Long userId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT COUNT(u.id) FROM users u JOIN roles r ON r.id = u.role_id " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE'", nativeQuery = true)
    Long countActiveEmployees();

    @Query(value = "SELECT r.name, COUNT(u.id) FROM users u JOIN roles r ON r.id = u.role_id " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' " +
           "GROUP BY r.name ORDER BY COUNT(u.id) DESC", nativeQuery = true)
    List<Object[]> countActiveEmployeesByRole();

    @Query(value = "SELECT u.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as emp_name, " +
           "b.name as branch_name, COALESCE(SUM(sp.paid_amount), 0) as revenue " +
           "FROM users u " +
           "JOIN roles r ON r.id = u.role_id " +
           "LEFT JOIN branches b ON b.id = u.branch_id " +
           "LEFT JOIN students s ON s.assigned_by_id = u.id " +
           "LEFT JOIN student_payments sp ON sp.student_id = s.id AND sp.is_deleted = false AND sp.created_at >= :from " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' " +
           "GROUP BY u.id, emp_name, b.name ORDER BY revenue DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findEmployeeLeaderboard(@Param("from") java.time.LocalDateTime from);

    @Query(value = "SELECT COUNT(u.id) FROM users u JOIN roles r ON r.id = u.role_id " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' " +
           "AND DATE_TRUNC('month', u.created_at) = DATE_TRUNC('month', CURRENT_TIMESTAMP - INTERVAL '1 month')", nativeQuery = true)
    Long countActiveEmployeesLastMonth();

    // ==================== BRANCH-SCOPED DASHBOARD QUERIES ====================

    @Query(value = "SELECT COUNT(u.id) FROM users u JOIN roles r ON r.id = u.role_id " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' AND u.branch_id = :branchId", nativeQuery = true)
    Long countActiveEmployeesForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT r.name, COUNT(u.id) FROM users u JOIN roles r ON r.id = u.role_id " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' AND u.branch_id = :branchId " +
           "GROUP BY r.name ORDER BY COUNT(u.id) DESC", nativeQuery = true)
    List<Object[]> countActiveEmployeesByRoleForBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT u.id, CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) as emp_name, " +
           "b.name as branch_name, COALESCE(SUM(sp.paid_amount), 0) as revenue " +
           "FROM users u " +
           "JOIN roles r ON r.id = u.role_id " +
           "LEFT JOIN branches b ON b.id = u.branch_id " +
           "LEFT JOIN students s ON s.assigned_by_id = u.id " +
           "LEFT JOIN student_payments sp ON sp.student_id = s.id AND sp.is_deleted = false AND sp.created_at >= :from " +
           "WHERE r.name NOT IN ('ADMIN','STUDENT','REFERRAL','COMPANY') AND u.status = 'ACTIVE' AND u.branch_id = :branchId " +
           "GROUP BY u.id, emp_name, b.name ORDER BY revenue DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findEmployeeLeaderboardForBranch(@Param("branchId") Long branchId, @Param("from") java.time.LocalDateTime from);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.reportingManager WHERE u.id = :userId")
    java.util.Optional<User> findByIdWithManager(@Param("userId") Long userId);

    /** Active employees assigned to a branch, for the Employee Tree (Part A1) - excludes non-employee roles. */
    @Query("SELECT u FROM User u WHERE u.branch IS NOT NULL AND u.role.name NOT IN :excludedRoles AND u.status = 'ACTIVE' " +
           "ORDER BY u.branch.id ASC, u.role.name ASC, u.firstName ASC")
    List<User> findActiveEmployeesForTree(@Param("excludedRoles") List<String> excludedRoles);

}
