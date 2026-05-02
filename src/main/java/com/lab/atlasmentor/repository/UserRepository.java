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
    
    @Query(value = "SELECT u.* FROM users u WHERE (:isAdmin = true OR u.branch_id = :branchId) AND u.is_deleted = false", nativeQuery = true)
    List<User> findAllActiveWithAccess(@Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
    @Query(value = "SELECT u.* FROM users u WHERE (:isAdmin = true OR u.branch_id = :branchId) AND u.role_id IN (SELECT r.id FROM roles r WHERE r.name IN :roleNames)", nativeQuery = true)
    List<User> findByRoleNamesWithAccess(@Param("roleNames") List<String> roleNames, @Param("isAdmin") boolean isAdmin, @Param("branchId") Long branchId);
    
}
