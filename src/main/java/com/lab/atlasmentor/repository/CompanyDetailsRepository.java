package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.CompanyDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDetailsRepository extends JpaRepository<CompanyDetails, Long> {
    
    Optional<CompanyDetails> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cd FROM CompanyDetails cd WHERE cd.assignedTo = :assignedTo")
    List<CompanyDetails> findByAssignedTo(@Param("assignedTo") Long assignedTo);
    
    void deleteByUserId(@Param("userId") Long userId);
}
