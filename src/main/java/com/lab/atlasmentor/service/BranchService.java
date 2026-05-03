package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.BranchRepository;
import com.lab.atlasmentor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    public Branch createBranch(Branch branch, Long managerId) {
        if (branch.getName() == null || branch.getName().trim().isEmpty()) {
            throw new RuntimeException("Branch name is required");
        }
        
        if (branch.getName().trim().length() < 2) {
            throw new RuntimeException("Branch name must be at least 2 characters long");
        }
        
        if (branch.getName().length() > 150) {
            throw new RuntimeException("Branch name must not exceed 150 characters");
        }
        
        if (branch.getLocation() != null && branch.getLocation().length() > 255) {
            throw new RuntimeException("Location must not exceed 255 characters");
        }
        
        if (branchRepository.existsByName(branch.getName().trim())) {
            throw new RuntimeException("Branch with name '" + branch.getName() + "' already exists");
        }
        
        branch.setName(branch.getName().trim());
        if (branch.getLocation() != null) {
            branch.setLocation(branch.getLocation().trim());
        }
        
        // Set manager if provided
        if (managerId != null) {
            Optional<User> optionalManager = userRepository.findById(managerId);
            if (optionalManager.isEmpty()) {
                throw new RuntimeException("Manager not found with id: " + managerId);
            }
            branch.setManager(optionalManager.get());
        }
        
        return branchRepository.save(branch);
    }

    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<Branch> getAllBranchesIncludingInactive() {
        return branchRepository.findAll();
    }

    public Branch updateBranch(Long id, Branch branchDetails, Long managerId) {
        Optional<Branch> optionalBranch = branchRepository.findById(id);
        if (optionalBranch.isEmpty()) {
            throw new RuntimeException("Branch not found with id: " + id);
        }

        Branch branch = optionalBranch.get();
        
        if (branchDetails.getName() == null || branchDetails.getName().trim().isEmpty()) {
            throw new RuntimeException("Branch name is required");
        }
        
        if (branchDetails.getName().trim().length() < 2) {
            throw new RuntimeException("Branch name must be at least 2 characters long");
        }
        
        if (branchDetails.getName().length() > 150) {
            throw new RuntimeException("Branch name must not exceed 150 characters");
        }
        
        if (branchDetails.getLocation() != null && branchDetails.getLocation().length() > 255) {
            throw new RuntimeException("Location must not exceed 255 characters");
        }
        
        if (!branch.getName().equals(branchDetails.getName().trim()) && 
            branchRepository.existsByName(branchDetails.getName().trim())) {
            throw new RuntimeException("Branch with name '" + branchDetails.getName() + "' already exists");
        }

        branch.setName(branchDetails.getName().trim());
        if (branchDetails.getLocation() != null) {
            branch.setLocation(branchDetails.getLocation().trim());
        } else {
            branch.setLocation(null);
        }
        branch.setStatus(branchDetails.getStatus());
        
        // Update manager if provided
        if (managerId != null) {
            Optional<User> optionalManager = userRepository.findById(managerId);
            if (optionalManager.isEmpty()) {
                throw new RuntimeException("Manager not found with id: " + managerId);
            }
            branch.setManager(optionalManager.get());
        } else {
            branch.setManager(null); // Allow removing manager
        }

        return branchRepository.save(branch);
    }

    public Branch changeBranchStatus(Long id, UserStatus status, User updatedBy) {
        Optional<Branch> optionalBranch = branchRepository.findById(id);
        if (optionalBranch.isEmpty()) {
            throw new RuntimeException("Branch not found with id: " + id);
        }

        Branch branch = optionalBranch.get();
        branch.setStatus(status);
        branch.setUpdatedBy(updatedBy.getId());
        
        return branchRepository.save(branch);
    }

    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new RuntimeException("Branch not found with id: " + id);
        }
        branchRepository.deleteById(id);
    }
}
