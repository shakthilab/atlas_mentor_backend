package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.repository.MobileCountryCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MobileCountryCodeService {

    @Autowired
    private MobileCountryCodeRepository repository;

    public List<MobileCountryCode> getAllMobileCountryCodes() {
        return repository.findAll();
    }

    public List<MobileCountryCode> getActiveMobileCountryCodes() {
        return repository.findByIsActive(true);
    }

    public Optional<MobileCountryCode> getByIsoAlpha2(String isoAlpha2) {
        return repository.findByIsoAlpha2(isoAlpha2);
    }

    public Optional<MobileCountryCode> getByIsoAlpha3(String isoAlpha3) {
        return repository.findByIsoAlpha3(isoAlpha3);
    }

    public List<MobileCountryCode> getByMobileCode(String mobileCode) {
        return repository.findByMobileCode(mobileCode);
    }

    public List<MobileCountryCode> searchByCountryName(String countryName) {
        return repository.findByCountryNameContainingIgnoreCase(countryName);
    }
}
