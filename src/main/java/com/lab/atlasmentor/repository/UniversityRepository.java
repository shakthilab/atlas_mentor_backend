package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {
    
    List<University> findByCountryId(Long countryId);
    
    List<University> findByCountryIdOrderByName(Long countryId);
    
    List<University> findByCountryIdAndActive(Long countryId, Boolean active);
    
    List<University> findByCountryIdAndActiveOrderByName(Long countryId, Boolean active);
    
    Optional<University> findByNameAndCountryId(String name, Long countryId);

    /** Case-insensitive, not scoped to a country — used by LeadImportService to resolve a
     * sheet's free-text "Target University" cell to a University id. */
    Optional<University> findByNameIgnoreCase(String name);
    
    Optional<University> findByNameAndCountryIdAndActive(String name, Long countryId, Boolean active);
    
    boolean existsByNameAndCountryId(String name, Long countryId);
    
    boolean existsByNameAndCountryIdAndActive(String name, Long countryId, Boolean active);
    
    long countByCountryId(Long countryId);
    
    long countByCountryIdAndActive(Long countryId, Boolean active);
    
    @Query("SELECT u FROM University u WHERE u.country.id = :countryId ORDER BY u.name")
    List<University> findByCountryIdOrderByNameAsc(@Param("countryId") Long countryId);
    
    @Query("SELECT u FROM University u WHERE u.country.id = :countryId AND u.active = true ORDER BY u.name")
    List<University> findActiveByCountryIdOrderByName(@Param("countryId") Long countryId);
    
    @Query("SELECT u FROM University u WHERE u.active = true ORDER BY u.name")
    List<University> findAllActive();
}
