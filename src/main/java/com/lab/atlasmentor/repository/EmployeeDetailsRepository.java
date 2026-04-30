package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.EmployeeDetails;
import com.lab.atlasmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDetailsRepository extends JpaRepository<EmployeeDetails, Long> {
    
    @Query("SELECT ed FROM EmployeeDetails ed WHERE ed.user.id = :userId")
    Optional<EmployeeDetails> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ed FROM EmployeeDetails ed WHERE ed.manager.id = :managerId")
    List<EmployeeDetails> findByManagerId(@Param("managerId") Long managerId);
    
    @Query("SELECT ed FROM EmployeeDetails ed WHERE ed.isSenior = true")
    List<EmployeeDetails> findSeniorEmployees();
}
