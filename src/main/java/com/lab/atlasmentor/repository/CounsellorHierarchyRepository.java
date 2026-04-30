package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.CounsellorHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounsellorHierarchyRepository extends JpaRepository<CounsellorHierarchy, Long> {
    
    Optional<CounsellorHierarchy> findByJuniorCounsellor_Id(Long juniorCounsellorId);
    
    List<CounsellorHierarchy> findBySeniorCounsellor_Id(Long seniorCounsellorId);
    
    @Query("SELECT ch.seniorCounsellor.id FROM CounsellorHierarchy ch WHERE ch.juniorCounsellor.id = :juniorCounsellorId")
    Optional<Long> findSeniorCounsellorIdByJuniorCounsellorId(@Param("juniorCounsellorId") Long juniorCounsellorId);
    
    @Query("SELECT ch.juniorCounsellor.id FROM CounsellorHierarchy ch WHERE ch.seniorCounsellor.id = :seniorCounsellorId")
    List<Long> findJuniorCounsellorIdsBySeniorCounsellorId(@Param("seniorCounsellorId") Long seniorCounsellorId);
    
    void deleteByJuniorCounsellor_Id(Long juniorCounsellorId);
    
    void deleteBySeniorCounsellor_Id(Long seniorCounsellorId);
}
