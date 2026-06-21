package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.CounsellorResponse;
import com.lab.atlasmentor.dto.ReferralCompanyUserResponse;
import com.lab.atlasmentor.dto.UserResponse;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<CounsellorResponse> getActiveCounsellorsByBranch(Long branchId) {
        List<User> counsellors = userRepository.findActiveCounsellorsByBranchId(branchId);
        
        return counsellors.stream().map(user -> {
            CounsellorResponse response = new CounsellorResponse();
            response.setId(user.getId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setFullName(user.getFullName());
            response.setEmail(user.getEmail());
            response.setPhone(user.getPhone());
            response.setRole(user.getRole() != null ? user.getRole().getName() : null);
            response.setBranchId(user.getBranch() != null ? user.getBranch().getId() : null);
            response.setBranchName(user.getBranch() != null ? user.getBranch().getName() : null);
            response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
            
            // Get student count for this counselor
            Long studentCount = userRepository.countStudentsByCounsellorId(user.getId());
            response.setStudentCount(studentCount != null ? studentCount : 0L);

            return response;
        }).collect(Collectors.toList());
    }

    public List<CounsellorResponse> getCounsellorsByBranch(Long branchId) {
        List<User> counsellors = userRepository.findSeniorAndJuniorCounsellorsByBranchId(branchId);

        return counsellors.stream().map(user -> {
            CounsellorResponse response = new CounsellorResponse();
            response.setId(user.getId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setFullName(user.getFullName());
            response.setEmail(user.getEmail());
            response.setPhone(user.getPhone());
            response.setRole(user.getRole() != null ? user.getRole().getName() : null);
            response.setBranchId(user.getBranch() != null ? user.getBranch().getId() : null);
            response.setBranchName(user.getBranch() != null ? user.getBranch().getName() : null);
            response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
            Long studentCount = userRepository.countStudentsByCounsellorId(user.getId());
            response.setStudentCount(studentCount != null ? studentCount : 0L);
            return response;
        }).collect(Collectors.toList());
    }

    public List<ReferralCompanyUserResponse> getActiveReferralsAndCompaniesByBranch(List<Long> roleIds, Long branchId) {
        List<User> users = userRepository.findActiveUsersByRoleIdsAndBranchId(roleIds, branchId);

        return users.stream().map(user -> {
            ReferralCompanyUserResponse response = new ReferralCompanyUserResponse();
            response.setId(user.getId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setFullName(user.getFullName());
            response.setEmail(user.getEmail());
            response.setPhone(user.getPhone());
            response.setRole(user.getRole() != null ? user.getRole().getName() : null);
            response.setRoleId(user.getRole() != null ? user.getRole().getId() : null);
            response.setBranchId(user.getBranch() != null ? user.getBranch().getId() : null);
            response.setBranchName(user.getBranch() != null ? user.getBranch().getName() : null);
            response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);

            return response;
        }).collect(Collectors.toList());
    }

    public List<UserResponse> getActiveUsersByRoleIdAndBranchId(Long roleId, Long branchId) {
        List<User> users = userRepository.findActiveUsersByRoleIdAndBranchId(roleId, branchId);

        return users.stream().map(user -> {
            // Build BranchResponse if user has a branch
            com.lab.atlasmentor.dto.BranchResponse branchResponse = null;
            if (user.getBranch() != null) {
                branchResponse = new com.lab.atlasmentor.dto.BranchResponse(
                        user.getBranch().getId(),
                        user.getBranch().getName(),
                        user.getBranch().getLocation(),
                        user.getBranch().getStatus(),
                        user.getBranch().getCreatedAt()
                );
            }

            return new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getStatus(),
                    user.getIsVerified(),
                    branchResponse,
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    null,  // referralType
                    null,  // companyDetails
                    null,  // assignedToUsers
                    null   // userCounts
            );
        }).collect(Collectors.toList());
    }
}
