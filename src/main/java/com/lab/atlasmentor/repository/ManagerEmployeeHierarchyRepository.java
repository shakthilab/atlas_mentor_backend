package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.ManagerEmployeeHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerEmployeeHierarchyRepository extends JpaRepository<ManagerEmployeeHierarchy, Long> {
    
    @Query("SELECT DISTINCT meh.employeeId FROM ManagerEmployeeHierarchy meh WHERE meh.managerId = :managerId")
    List<Long> findEmployeeIdsByManagerId(@Param("managerId") Long managerId);
    
    @Query("SELECT DISTINCT meh.managerId FROM ManagerEmployeeHierarchy meh WHERE meh.employeeId = :employeeId")
    Long findManagerIdByEmployeeId(@Param("employeeId") Long employeeId);
    
    @Query("SELECT meh FROM ManagerEmployeeHierarchy meh WHERE meh.managerId = :managerId")
    List<ManagerEmployeeHierarchy> findByManagerId(@Param("managerId") Long managerId);
    
    @Query("SELECT meh FROM ManagerEmployeeHierarchy meh WHERE meh.employeeId = :employeeId")
    ManagerEmployeeHierarchy findByEmployeeId(@Param("employeeId") Long employeeId);
    
    boolean existsByEmployeeId(Long employeeId);

    void deleteByEmployeeId(Long employeeId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ManagerEmployeeHierarchy meh WHERE meh.employeeId = :employeeId")
    void deleteDirectlyByEmployeeId(@Param("employeeId") Long employeeId);
}
