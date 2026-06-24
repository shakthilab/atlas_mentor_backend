package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.UserReporting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReportingRepository extends JpaRepository<UserReporting, Long> {

    List<UserReporting> findByManagerId(Long managerId);

    List<UserReporting> findByEmployeeId(Long employeeId);

    List<UserReporting> findByBranchId(Long branchId);

    Optional<UserReporting> findByManagerIdAndEmployeeId(Long managerId, Long employeeId);

    boolean existsByManagerIdAndEmployeeId(Long managerId, Long employeeId);

    @Query("SELECT ur FROM UserReporting ur JOIN FETCH ur.manager JOIN FETCH ur.employee WHERE ur.manager.id = :managerId")
    List<UserReporting> findByManagerIdWithDetails(@Param("managerId") Long managerId);

    @Query("SELECT ur FROM UserReporting ur WHERE ur.employee.id = :employeeId ORDER BY ur.createdAt DESC")
    List<UserReporting> findReportingChainForEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT ur.employee.id FROM UserReporting ur WHERE ur.manager.id = :managerId")
    List<Long> findDirectReporteeIds(@Param("managerId") Long managerId);

    @Query("SELECT ur.employee.id FROM UserReporting ur WHERE ur.manager.id = :managerId AND ur.branch.id = :branchId")
    List<Long> findDirectReporteeIdsByBranch(@Param("managerId") Long managerId, @Param("branchId") Long branchId);

    void deleteByManagerIdAndEmployeeId(Long managerId, Long employeeId);
}
