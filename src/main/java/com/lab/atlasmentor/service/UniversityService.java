package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.University;
import com.lab.atlasmentor.model.Country;
import com.lab.atlasmentor.repository.UniversityRepository;
import com.lab.atlasmentor.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UniversityService {

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public List<University> getAllUniversities() {
        return universityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<University> getAllActiveUniversities() {
        return universityRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Optional<University> getUniversityById(Long id) {
        return universityRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<University> getUniversitiesByCountryId(Long countryId) {
        return universityRepository.findByCountryIdOrderByNameAsc(countryId);
    }

    @Transactional(readOnly = true)
    public List<University> getActiveUniversitiesByCountryId(Long countryId) {
        return universityRepository.findActiveByCountryIdOrderByName(countryId);
    }

    @Transactional
    public University createUniversity(University university) {
        return universityRepository.save(university);
    }

    @Transactional
    public University createUniversity(String name, Long countryId) {
        Optional<Country> countryOpt = countryRepository.findById(countryId);
        if (countryOpt.isPresent()) {
            University university = new University(name, countryOpt.get());
            return universityRepository.save(university);
        }
        return null;
    }

    @Transactional
    public University updateUniversity(Long id, University universityDetails) {
        Optional<University> existingUniversityOpt = universityRepository.findById(id);
        if (existingUniversityOpt.isPresent()) {
            University existingUniversity = existingUniversityOpt.get();
            existingUniversity.setName(universityDetails.getName());
            if (universityDetails.getCountry() != null) {
                existingUniversity.setCountry(universityDetails.getCountry());
            }
            return universityRepository.save(existingUniversity);
        }
        return null;
    }

    @Transactional
    public boolean deleteUniversity(Long id) {
        if (universityRepository.existsById(id)) {
            universityRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCountryId(String name, Long countryId) {
        return universityRepository.existsByNameAndCountryId(name, countryId);
    }

    @Transactional(readOnly = true)
    public long countByCountryId(Long countryId) {
        return universityRepository.countByCountryId(countryId);
    }
}
