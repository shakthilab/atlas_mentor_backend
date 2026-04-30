package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.MobileCountryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileCountryCodeRepository extends JpaRepository<MobileCountryCode, Long> {
    
    Optional<MobileCountryCode> findByIsoAlpha2(String isoAlpha2);
    
    Optional<MobileCountryCode> findByIsoAlpha3(String isoAlpha3);
    
    List<MobileCountryCode> findByMobileCode(String mobileCode);
    
    List<MobileCountryCode> findByCountryNameContainingIgnoreCase(String countryName);
    
    List<MobileCountryCode> findByIsActive(Boolean isActive);
    
    @Query("SELECT m FROM MobileCountryCode m WHERE m.mobileCode = :mobileCode AND m.isActive = true")
    List<MobileCountryCode> findActiveByMobileCode(@Param("mobileCode") String mobileCode);
    
    @Query("SELECT m FROM MobileCountryCode m WHERE m.countryName = :countryName AND m.isActive = true")
    Optional<MobileCountryCode> findActiveByCountryName(@Param("countryName") String countryName);
    
    boolean existsByIsoAlpha2(String isoAlpha2);
    
    boolean existsByIsoAlpha3(String isoAlpha3);
}
