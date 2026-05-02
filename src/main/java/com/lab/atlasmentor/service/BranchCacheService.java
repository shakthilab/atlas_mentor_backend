package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BranchCacheService {

    @Autowired
    private BranchRepository branchRepository;

    private Map<Long, Branch> branchCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        refreshCache();
    }

    public void refreshCache() {
        branchCache.clear();
        
        branchRepository.findAll().forEach(branch -> {
            branchCache.put(branch.getId(), branch);
        });
    }

    public Branch getBranchById(Long branchId) {
        Branch branch = branchCache.get(branchId);
        if (branch == null) {
            // Fallback to database if not in cache
            branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + branchId));
            branchCache.put(branchId, branch);
        }
        return branch;
    }

    public void clearCache() {
        branchCache.clear();
    }

    public Map<Long, Branch> getAllBranchesCache() {
        return new ConcurrentHashMap<>(branchCache);
    }
}
