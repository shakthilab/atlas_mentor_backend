package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    
    Optional<Country> findByCode(String code);
    
    Optional<Country> findByName(String name);

    /** Case-insensitive name lookup — used by LeadImportService to resolve a sheet's free-text
     * "Destination Country" cell to a Country id without requiring an exact-case match. */
    Optional<Country> findByNameIgnoreCase(String name);

    Optional<Country> findByCodeAndActive(String code, Boolean active);
    
    Optional<Country> findByNameAndActive(String name, Boolean active);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Country c WHERE c.active = true ORDER BY c.name")
    List<Country> findAllActive();
}
