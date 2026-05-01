package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.Country;
import com.lab.atlasmentor.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CountryService {

    @Autowired
    private CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Country> getAllActiveCountries() {
        return countryRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Optional<Country> getCountryById(Long id) {
        return countryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Country> getCountryByCode(String code) {
        return countryRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Optional<Country> getActiveCountryByCode(String code) {
        return countryRepository.findByCodeAndActive(code, true);
    }

    @Transactional(readOnly = true)
    public Optional<Country> getCountryByName(String name) {
        return countryRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Optional<Country> getActiveCountryByName(String name) {
        return countryRepository.findByNameAndActive(name, true);
    }

    @Transactional
    public Country createCountry(Country country) {
        return countryRepository.save(country);
    }

    @Transactional
    public Country updateCountry(Long id, Country countryDetails) {
        Optional<Country> existingCountryOpt = countryRepository.findById(id);
        if (existingCountryOpt.isPresent()) {
            Country existingCountry = existingCountryOpt.get();
            existingCountry.setName(countryDetails.getName());
            existingCountry.setCode(countryDetails.getCode());
            return countryRepository.save(existingCountry);
        }
        return null;
    }

    @Transactional
    public boolean deleteCountry(Long id) {
        if (countryRepository.existsById(id)) {
            countryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return countryRepository.existsByCode(code);
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return countryRepository.existsByName(name);
    }
}
