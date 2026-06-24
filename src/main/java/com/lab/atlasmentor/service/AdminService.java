package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.exception.EmailAlreadyExistsException;
import com.lab.atlasmentor.exception.UserNotFoundException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.util.PasswordGenerator;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AdminService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReferralDetailsRepository referralDetailsRepository;

    @Autowired
    private BranchRepository branchRepository;

    // SecurityUtil removed - now using SecurityUtils directly

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private ManagerEmployeeHierarchyRepository managerEmployeeHierarchyRepository;

    @Autowired
    private MobileCountryCodeRepository mobileCountryCodeRepository;

    @Autowired
    private ReferralAssignmentRepository referralAssignmentRepository;

    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClientPayoutRepository clientPayoutRepository;

    @Autowired
    private ClientPayoutActivityRepository clientPayoutActivityRepository;

    @Autowired
    private ReferralResourceRepository referralResourceRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request, String createdByRoleName) {
        // Validate role creation permissions
        validateRoleCreation(request.getRole(), createdByRoleName);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setBranchId(request.getBranchId());
        user.setIsVerified(true); // Admin-created users are pre-verified

        // Set role using Role entity
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
        user.setRole(role);
        User savedUser = userRepository.save(user);
        
        return convertToUserResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersExcludingAdminAndStudent(List<Long> roleIds, Long branchId) {
        List<String> excludedRoles = List.of("ADMIN", "STUDENT");
        List<User> users;
        
        if (roleIds != null && !roleIds.isEmpty() && branchId != null) {
            users = userRepository.findUsersExcludingRolesWithRoleIdsAndBranchId(excludedRoles, roleIds, branchId);
        } else if (roleIds != null && !roleIds.isEmpty()) {
            users = userRepository.findUsersExcludingRolesWithRoleIds(excludedRoles, roleIds);
        } else if (branchId != null) {
            users = userRepository.findUsersExcludingRolesWithBranchId(excludedRoles, branchId);
        } else {
            users = userRepository.findUsersExcludingRoles(excludedRoles);
        }
        
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersExcludingAdminAndStudent(Long roleId) {
        return getUsersExcludingAdminAndStudent(roleId != null ? List.of(roleId) : null, null);
    }

    public List<UserResponse> getUsersExcludingAdminAndStudent() {
        return getUsersExcludingAdminAndStudent(null, null);
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(roleName))
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<ManagerResponse> getAllManagers() {
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole("MANAGER") || user.hasRole("BRANCH_PARTNER") || user.hasRole("ADMINISTRATIVE_ASSISTANT"))
                .filter(user -> user.getStatus() == com.lab.atlasmentor.enums.UserStatus.ACTIVE)
                .map(this::convertToManagerResponse)
                .collect(Collectors.toList());
    }

    public List<SeniorCounsellorResponse> getAllActiveSeniorCounsellors() {
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole("SENIOR_COUNSELLOR"))
                .filter(user -> user.getStatus() == com.lab.atlasmentor.enums.UserStatus.ACTIVE)
                .map(this::convertToSeniorCounsellorResponse)
                .collect(Collectors.toList());
    }

    public List<UnassignedEmployeeResponse> getUnassignedEmployees(Long managerId) {
        List<Long> assignedEmployeeIds = managerEmployeeHierarchyRepository.findAll().stream()
                .map(ManagerEmployeeHierarchy::getEmployeeId)
                .collect(Collectors.toList());

        List<String> employeeRoles = List.of("VIDEO_EDITOR");

        // If managerId is provided, resolve the branch from that manager and filter by it
        Long filterBranchId = null;
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new BusinessException("Manager not found with id: " + managerId));
            filterBranchId = manager.getBranchId();
            log.info("Filtering unassigned employees by managerId={} branchId={}", managerId, filterBranchId);
        } else {
            try {
                var currentUser = SecurityUtils.getCurrentUser();
                if (!currentUser.isAdmin()) {
                    filterBranchId = currentUser.getBranchId();
                }
                log.info("Getting unassigned employees: userId={}, role={}, branchId={}, isAdmin={}",
                    currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            } catch (Exception e) {
                log.error("Error getting current user for unassigned employees: {}", e.getMessage(), e);
            }
        }

        final Long branchFilter = filterBranchId;
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == com.lab.atlasmentor.enums.UserStatus.ACTIVE)
                .filter(user -> !assignedEmployeeIds.contains(user.getId()))
                .filter(user -> user.getRole() != null && employeeRoles.contains(user.getRole().getName()))
                .filter(user -> branchFilter == null ||
                    (user.getBranchId() != null && user.getBranchId().equals(branchFilter)))
                .map(this::convertToUnassignedEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long userId, CreateUserRequest request, String createdByRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate role update permissions
        validateRoleCreation(request.getRole(), createdByRoleName);

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setBranchId(request.getBranchId());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update role using Role entity
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        
        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("User not found");
        }
        userRepository.deleteById(userId);
    }

    private void validateRoleCreation(String roleToCreateName, String createdByRoleName) {
        // Admin can create all roles except another admin
        if ("ADMIN".equals(createdByRoleName) && "ADMIN".equals(roleToCreateName)) {
            throw new BusinessException("Admin cannot create another admin user");
        }

        if ("ADMIN".equals(createdByRoleName)) {
            // Admin can create: MANAGER
            if ("STUDENT".equals(roleToCreateName)) {
                throw new BusinessException("Students must register themselves");
            }
        } else if ("MANAGER".equals(createdByRoleName) || "ADMINISTRATIVE_ASSISTANT".equals(createdByRoleName)) {
            // Manager can create: JUNIOR_COUNSELLOR, COUNSELLOR, VIDEO_EDITOR
            if (!List.of("JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "VIDEO_EDITOR").contains(roleToCreateName)) {
                throw new BusinessException("Manager can only create JUNIOR_COUNSELLOR, SENIOR_COUNSELLOR, or VIDEO_EDITOR users");
            }
        } else {
            throw new BusinessException("Only ADMIN and MANAGER can create users");
        }
    }

    @Transactional
    public User createReferral(ReferralRequest referralRequest, HttpServletRequest request) {
        System.out.println("Creating referral with data:");
        System.out.println("Name: " + referralRequest.getName());
        System.out.println("Email: " + referralRequest.getEmail());
        System.out.println("Phone: " + referralRequest.getPhone());
        System.out.println("ReferralType: " + referralRequest.getReferralType());
        System.out.println("BranchId: " + referralRequest.getBranchId());
        
        if (userRepository.existsByEmail(referralRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Generate random password
        String generatedPassword = PasswordGenerator.generateRandomPassword();

        User user = new User();
        // Split name into firstName and lastName
        String fullName = referralRequest.getName();
        if (fullName != null && fullName.trim().contains(" ")) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
        } else {
            user.setFirstName(fullName);
            user.setLastName(null);
        }
        user.setEmail(referralRequest.getEmail());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setPhone(referralRequest.getPhone());
        // Set branch entity if branchId is provided
        if (referralRequest.getBranchId() != null) {
            Branch branch = branchRepository.findById(referralRequest.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + referralRequest.getBranchId()));
            user.setBranch(branch);
        }
        user.setIsVerified(true); // Referrals are pre-verified
        
        // Set createdBy from current admin/manager user
        String token = request.getHeader("Authorization");
        User currentUser = null;
        if (token != null && token.startsWith("Bearer ")) {
            currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            user.setCreatedBy(currentUser.getId());
            user.setUpdatedBy(currentUser.getId());
        }
        
        // Set REFERRAL role using Role entity
        Role referralRole = roleRepository.findByName("REFERRAL")
                .orElseThrow(() -> new RuntimeException("REFERRAL role not found"));
        user.setRole(referralRole);
        User savedUser = userRepository.save(user);
        
        // Create referral details
        try {
            System.out.println("Step 1: Creating ReferralDetails object");
            ReferralDetails referralDetails = new ReferralDetails(savedUser, referralRequest.getReferralType());
            
            // Set createdBy and updatedBy for ReferralDetails using existing currentUser
            if (currentUser != null) {
                referralDetails.setCreatedBy(currentUser.getId());
                referralDetails.setUpdatedBy(currentUser.getId());
            }
            
            System.out.println("Step 2: ReferralDetails created - userId: " + referralDetails.getUserId() + ", type: " + referralDetails.getReferralType());
            System.out.println("Step 3: About to save ReferralDetails");
            ReferralDetails savedReferralDetails = referralDetailsRepository.save(referralDetails);
            System.out.println("Step 4: ReferralDetails saved successfully with ID: " + savedReferralDetails.getId());
            System.out.println("Step 5: ReferralDetails saved - userId: " + savedReferralDetails.getUserId() + ", type: " + savedReferralDetails.getReferralType());
            
            // Handle multiple assignments if provided
            if (referralRequest.getAssignedToIds() != null && !referralRequest.getAssignedToIds().isEmpty()) {
                System.out.println("Step 6: Creating referral assignments");
                for (Long assignedToId : referralRequest.getAssignedToIds()) {
                    try {
                        // Validate that assignedTo user exists
                        User assignedToUser = userRepository.findById(assignedToId)
                            .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + assignedToId));
                        
                        ReferralAssignment assignment = new ReferralAssignment(savedUser, assignedToUser);
                        if (currentUser != null) {
                            assignment.setCreatedBy(currentUser.getId());
                            assignment.setUpdatedBy(currentUser.getId());
                        }
                        
                        referralAssignmentRepository.save(assignment);
                        System.out.println("Assignment created: referralId=" + savedUser.getId() + ", assignedToId=" + assignedToId);
                    } catch (Exception e) {
                        System.err.println("Error creating assignment for user " + assignedToId + ": " + e.getMessage());
                        // Continue with other assignments even if one fails
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR in ReferralDetails save process:");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new RuntimeException("Failed to save referral details: " + e.getMessage());
        }
        
        // Send credentials email - Temporarily commented out for testing
         emailService.sendEmployeeCredentialsEmail(
             savedUser.getEmail(),
             savedUser.getFullName(),
             generatedPassword
         );
        
        return savedUser;
    }

    @Transactional
    public User createCompany(CompanyRequest companyRequest, HttpServletRequest request) {
        if (userRepository.existsByEmail(companyRequest.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        // Generate random password
        String generatedPassword = PasswordGenerator.generateRandomPassword();

        User user = new User();
        // Split name into firstName and lastName
        String fullName = companyRequest.getName();
        if (fullName != null && fullName.trim().contains(" ")) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
        } else {
            user.setFirstName(fullName);
            user.setLastName(null);
        }
        user.setEmail(companyRequest.getEmail());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setPhone(companyRequest.getPhone());
        // Set branch entity if branchId is provided
        if (companyRequest.getBranchId() != null) {
            Branch branch = branchRepository.findById(companyRequest.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + companyRequest.getBranchId()));
            user.setBranch(branch);
        }
        user.setIsVerified(true); // Companies are pre-verified
        
        // Set createdBy from current admin/manager user
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            user.setCreatedBy(currentUser.getId());
            user.setUpdatedBy(currentUser.getId());
        }
        
        // Set COMPANY role using Role entity
        Role companyRole = roleRepository.findByName("COMPANY")
                .orElseThrow(() -> new RuntimeException("COMPANY role not found"));
        user.setRole(companyRole);
        User savedUser = userRepository.save(user);
        
        // Create CompanyDetails entry
        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails.setUser(savedUser);
        companyDetails.setCompanyName(companyRequest.getName());
        companyDetails.setContactPerson(companyRequest.getContactPerson());
        companyDetails.setAddress(companyRequest.getAddress());
        companyDetails.setWebsite(companyRequest.getWebsite());
        companyDetails.setIndustry(companyRequest.getIndustry());
        
        // Note: assignedTo should only be managed through hierarchy assignment API
        // Not during company creation
        
        // Set createdBy and updatedBy for CompanyDetails
        if (token != null && token.startsWith("Bearer ")) {
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            companyDetails.setCreatedBy(currentUser.getId());
            companyDetails.setUpdatedBy(currentUser.getId());
        }
        
        companyDetailsRepository.save(companyDetails);
        
        // Send credentials email
        emailService.sendEmployeeCredentialsEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            generatedPassword
        );
        
        return savedUser;
    }

    public PageResponse<UserResponse> getReferrals(int page, int size, String search, String referralType, Long branchId) {
        try {
            // Get current user for branch-based filtering
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Getting referrals with branch-based access control: userId={}, role={}, branchId={}, isAdmin={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            
            // Apply branch-based filtering for non-admin users
            Long effectiveBranchId = branchId;
            if (!currentUser.isAdmin() && branchId == null) {
                effectiveBranchId = currentUser.getBranchId();
            }
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Get user IDs filtered by referral type if provided
        List<Long> filteredUserIds = null;
        if (referralType != null && !referralType.isEmpty()) {
            try {
                com.lab.atlasmentor.enums.ReferralType type = com.lab.atlasmentor.enums.ReferralType.valueOf(referralType.toUpperCase());
                filteredUserIds = referralDetailsRepository.findUserIdsByReferralType(type);
            } catch (IllegalArgumentException e) {
                return PageResponse.of(List.of(), page, size, 0);
            }
        }
        
        // Create effectively final copy for lambda expression
        final List<Long> finalFilteredUserIds = filteredUserIds;
        final Long finalEffectiveBranchId = effectiveBranchId;
        
        // Get users with REFERRAL role
        Page<User> referrals = userRepository.findAll().stream()
                .filter(user -> user.hasRole("REFERRAL"))
                .filter(user -> finalFilteredUserIds == null || finalFilteredUserIds.contains(user.getId()))
                .filter(user -> finalEffectiveBranchId == null || user.getBranchId() != null && user.getBranchId().equals(finalEffectiveBranchId))
                .filter(user -> search == null || search.isEmpty() || 
                        user.getFullName().toLowerCase().contains(search.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            int start = page * size;
                            int end = Math.min(start + size, list.size());
                            if (start >= list.size()) {
                                return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, list.size());
                            }
                            return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
                        }
                ));
        
        // Convert Page<User> to PageResponse<UserResponse>
        List<UserResponse> userResponses = referrals.getContent().stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
        
        return PageResponse.of(
            userResponses,
            referrals.getNumber(),
            referrals.getSize(),
            referrals.getTotalElements()
        );
        } catch (Exception e) {
            log.error("Error getting current user for referral filtering: {}", e.getMessage(), e);
            // Fallback to original behavior without branch filtering
            Pageable pageable = PageRequest.of(page, size);
            
            // Get user IDs filtered by referral type if provided
            List<Long> filteredUserIds = null;
            if (referralType != null && !referralType.isEmpty()) {
                try {
                    com.lab.atlasmentor.enums.ReferralType type = com.lab.atlasmentor.enums.ReferralType.valueOf(referralType.toUpperCase());
                    filteredUserIds = referralDetailsRepository.findUserIdsByReferralType(type);
                } catch (IllegalArgumentException ex) {
                    return PageResponse.of(List.of(), page, size, 0);
                }
            }
            
            final List<Long> finalFilteredUserIds = filteredUserIds;
            
            // Get users with REFERRAL role using original logic
            Page<User> referrals = userRepository.findAll().stream()
                    .filter(user -> user.hasRole("REFERRAL"))
                    .filter(user -> finalFilteredUserIds == null || finalFilteredUserIds.contains(user.getId()))
                    .filter(user -> branchId == null || user.getBranchId() != null && user.getBranchId().equals(branchId))
                    .filter(user -> search == null || search.isEmpty() || 
                            user.getFullName().toLowerCase().contains(search.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                int start = page * size;
                                int end = Math.min(start + size, list.size());
                                if (start >= list.size()) {
                                    return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, list.size());
                                }
                                return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
                            }
                    ));
            
            // Convert Page<User> to PageResponse<UserResponse>
            List<UserResponse> userResponses = referrals.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
            
            return PageResponse.of(
                userResponses,
                referrals.getNumber(),
                referrals.getSize(),
                referrals.getTotalElements()
            );
        }
    }

    public PageResponse<UserResponse> getCompanies(int page, int size, String search, Long branchId) {
        try {
            // Get current user for branch-based filtering
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Getting companies with branch-based access control: userId={}, role={}, branchId={}, isAdmin={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            
            // Apply branch-based filtering for non-admin users
            Long effectiveBranchId = branchId;
            if (!currentUser.isAdmin() && branchId == null) {
                effectiveBranchId = currentUser.getBranchId();
            }
            
            final Long finalEffectiveBranchId = effectiveBranchId;
            final String finalSearch = search;
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Get users with COMPANY role
            Page<User> companies = userRepository.findAll().stream()
                    .filter(user -> user.hasRole("COMPANY"))
                    .filter(user -> finalEffectiveBranchId == null || user.getBranchId() != null && user.getBranchId().equals(finalEffectiveBranchId))
                    .filter(user -> finalSearch == null || finalSearch.isEmpty() || 
                            user.getFullName().toLowerCase().contains(finalSearch.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(finalSearch.toLowerCase()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                int start = page * size;
                                int end = Math.min(start + size, list.size());
                                if (start >= list.size()) {
                                    return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, list.size());
                                }
                                return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
                            }
                    ));
            
            // Convert Page<User> to PageResponse<UserResponse>
            List<UserResponse> userResponses = companies.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
            
            return PageResponse.of(
                userResponses,
                companies.getNumber(),
                companies.getSize(),
                companies.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Error getting current user for company filtering: {}", e.getMessage(), e);
            // Fallback to original behavior without branch filtering
            Pageable pageable = PageRequest.of(page, size);
            
            // Get users with COMPANY role using original logic
            Page<User> companies = userRepository.findAll().stream()
                    .filter(user -> user.hasRole("COMPANY"))
                    .filter(user -> branchId == null || user.getBranchId() != null && user.getBranchId().equals(branchId))
                    .filter(user -> search == null || search.isEmpty() || 
                            user.getFullName().toLowerCase().contains(search.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                int start = page * size;
                                int end = Math.min(start + size, list.size());
                                if (start >= list.size()) {
                                    return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, list.size());
                                }
                                return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
                            }
                    ));
            
            // Convert Page<User> to PageResponse<UserResponse>
            List<UserResponse> userResponses = companies.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
            
            return PageResponse.of(
                userResponses,
                companies.getNumber(),
                companies.getSize(),
                companies.getTotalElements()
            );
        }
    }

    public User getCompanyById(Long companyId) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new BusinessException("User is not a company");
        }
        
        return company;
    }

    public UserResponse convertToUserResponse(User user) {
        Role primaryRole = user.getRole();
        
        // Get referral type and assignedTo for users with REFERRAL role
        com.lab.atlasmentor.enums.ReferralType referralType = null;
        List<UserResponse> assignedToUsers = null;
        if (primaryRole != null && "REFERRAL".equals(primaryRole.getName())) {
            referralType = referralDetailsRepository.findByUserId(user.getId())
                    .map(ReferralDetails::getReferralType)
                    .orElse(null);
            
            // Get multiple assigned users using ReferralAssignment
            List<User> assignedUsersList = referralAssignmentRepository.findAssignedUsersByReferralId(user.getId());
            if (assignedUsersList != null && !assignedUsersList.isEmpty()) {
                assignedToUsers = assignedUsersList.stream()
                        .map(assignedUser -> convertToUserResponse(assignedUser))
                        .collect(Collectors.toList());
            }
        }
        
        // Get company details for users with COMPANY role
        CompanyDetailsResponse companyDetails = null;
        if (primaryRole != null && "COMPANY".equals(primaryRole.getName())) {
            companyDetails = companyDetailsRepository.findByUserId(user.getId())
                    .map(cd -> {
                        UserResponse assignedToUser = null;
                        if (cd.getAssignedTo() != null) {
                            assignedToUser = convertToUserResponse(cd.getAssignedTo());
                        }
                        return new CompanyDetailsResponse(
                                cd.getId(),
                                cd.getUser().getId(),
                                cd.getCompanyName(),
                                cd.getContactPerson(),
                                cd.getAddress(),
                                cd.getWebsite(),
                                cd.getIndustry(),
                                assignedToUser,
                                cd.getCreatedAt(),
                                cd.getUpdatedAt()
                        );
                    })
                    .orElse(null);
        }
        
        // Get branch object
        BranchResponse branch = null;
        if (user.getBranchId() != null) {
            branch = branchRepository.findById(user.getBranchId())
                    .map(b -> new BranchResponse(b.getId(), b.getName(), b.getLocation(), b.getStatus(), b.getCreatedAt()))
                    .orElse(null);
        }
        
        String fullName = user.getFullName();
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        
        // For companies, use the company name from companyDetails instead of split firstName/lastName
        if (primaryRole != null && "COMPANY".equals(primaryRole.getName()) && companyDetails != null) {
            firstName = companyDetails.getCompanyName();
            lastName = null; // Companies don't need lastName
        }
        
        // Get staff and student counts for companies; leads/registered counts for referrals
        UserResponse.UserCounts userCounts = null;
        if (primaryRole != null && "COMPANY".equals(primaryRole.getName()) && user.getBranchId() != null) {
            List<String> staffRoles = List.of("MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR");
            Long totalStaffs = userRepository.countStaffsByBranchId(user.getBranchId(), staffRoles);
            Long totalStudents = userRepository.countStudentsByBranchId(user.getBranchId());
            userCounts = new UserResponse.UserCounts(totalStaffs, totalStudents);
        } else if (primaryRole != null && "REFERRAL".equals(primaryRole.getName())) {
            Long leadsCount = studentRepository.countByReferralIdAndStatus(user.getId(), "LEAD");
            Long registeredCount = studentRepository.countByReferralIdAndStatus(user.getId(), "REGISTERED");
            userCounts = new UserResponse.UserCounts(null, null, leadsCount, registeredCount);
        }
        
        return new UserResponse(
                user.getId(),
                firstName,
                lastName,
                user.getEmail(),
                user.getPhone(),
                primaryRole,
                user.getStatus(),
                user.getIsVerified(),
                branch,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                referralType,
                companyDetails,
                assignedToUsers,
                userCounts
        );
    }

    private ManagerResponse convertToManagerResponse(User user) {
        // Get branch object
        BranchResponse branch = null;
        if (user.getBranchId() != null) {
            branch = branchRepository.findById(user.getBranchId())
                    .map(b -> new BranchResponse(b.getId(), b.getName(), b.getLocation(), b.getStatus(), b.getCreatedAt()))
                    .orElse(null);
        }
        
        return new ManagerResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                branch,
                user.getCreatedAt()
        );
    }

    private SeniorCounsellorResponse convertToSeniorCounsellorResponse(User user) {
        return new SeniorCounsellorResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getBranchId(),
                user.getStatus().toString(),
                user.getIsVerified(),
                user.getRole().getName()
        );
    }

    private UnassignedEmployeeResponse convertToUnassignedEmployeeResponse(User user) {
        // Get branch object
        BranchResponse branch = null;
        if (user.getBranchId() != null) {
            branch = branchRepository.findById(user.getBranchId())
                    .map(b -> new BranchResponse(b.getId(), b.getName(), b.getLocation(), b.getStatus(), b.getCreatedAt()))
                    .orElse(null);
        }
        
        return new UnassignedEmployeeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getRole().getName(),
                branch,
                user.getCreatedAt()
        );
    }

    @Transactional
    public void deleteReferral(Long referralId) {
        User referral = userRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));

        if (!referral.hasRole("REFERRAL")) {
            throw new BusinessException("User is not a referral");
        }

        // Nullify soft FKs in client_payouts that point to this user (non-owner columns)
        clientPayoutRepository.nullifyDisputedByUserId(referralId);
        clientPayoutRepository.nullifyRespondedByUserId(referralId);
        clientPayoutRepository.nullifyAssignedByUserId(referralId);
        clientPayoutRepository.nullifyLastPaidByUserId(referralId);

        // Delete payout activities where this user acted (doneBy) or owns the payout
        clientPayoutActivityRepository.deleteByDoneByUserId(referralId);
        clientPayoutActivityRepository.deleteByClientPayoutUserId(referralId);

        // Delete client payouts owned by this referral (user_id FK)
        clientPayoutRepository.deleteByUserId(referralId);

        // Remove user from referral_resource_owners join table
        referralResourceRepository.deleteOwnerByUserId(referralId);

        // Delete refresh tokens (FK on user_id)
        refreshTokenRepository.deleteByUserId(referralId);

        // Delete referral assignments
        referralAssignmentRepository.deleteByReferralId(referralId);

        // Delete manager-employee hierarchy entry if referral was assigned to a manager
        managerEmployeeHierarchyRepository.deleteByEmployeeId(referralId);

        // Delete referral details
        referralDetailsRepository.findByUserId(referralId)
                .ifPresent(referralDetailsRepository::delete);

        // Delete employee details
        employeeDetailsRepository.deleteByUser_Id(referralId);

        // Delete user
        userRepository.delete(referral);
    }

    @Transactional
    public User updateReferral(Long referralId, ReferralRequest request) {
        // Check if user exists and has REFERRAL role
        User referral = userRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));
        
        if (!referral.hasRole("REFERRAL")) {
            throw new BusinessException("User is not a referral");
        }
        
        // Update user details
        // Split name into firstName and lastName
        String fullName = request.getName();
        if (fullName != null && fullName.trim().contains(" ")) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            referral.setFirstName(nameParts[0]);
            referral.setLastName(nameParts[1]);
        } else {
            referral.setFirstName(fullName);
            referral.setLastName(null);
        }
        referral.setEmail(request.getEmail());
        referral.setPhone(request.getPhone());
        // Set branch entity if branchId is provided
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + request.getBranchId()));
            referral.setBranch(branch);
        } else {
            referral.setBranch(null);
        }
        
        // Update referral details if referral type is provided
        if (request.getReferralType() != null) {
            ReferralDetails referralDetails = referralDetailsRepository.findByUserId(referralId)
                    .orElse(new ReferralDetails());
            
            // Set the user entity
            if (referralDetails.getUser() == null) {
                User user = new User();
                user.setId(referralId);
                referralDetails.setUser(user);
            }
            referralDetails.setReferralType(request.getReferralType());
            referralDetailsRepository.save(referralDetails);
        }
        
        return userRepository.save(referral);
    }

    public User updateReferralStatus(Long referralId, com.lab.atlasmentor.enums.UserStatus status) {
        // Check if user exists and has REFERRAL role
        User referral = userRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));
        
        if (!referral.hasRole("REFERRAL")) {
            throw new BusinessException("User is not a referral");
        }
        
        // Set status to the provided status
        referral.setStatus(status);
        
        return userRepository.save(referral);
    }

    public User deactivateReferral(Long referralId) {
        return updateReferralStatus(referralId, com.lab.atlasmentor.enums.UserStatus.INACTIVE);
    }

    @Transactional
    public User updateCompany(Long companyId, CompanyEditRequest editRequest) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new BusinessException("User is not a company");
        }
        
        // Check if email is being changed and if new email already exists
        if (!company.getEmail().equals(editRequest.getEmail()) && 
            userRepository.existsByEmail(editRequest.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        
        // Update company details
        // Split name into firstName and lastName
        String fullName = editRequest.getName();
        if (fullName != null && fullName.trim().contains(" ")) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            company.setFirstName(nameParts[0]);
            company.setLastName(nameParts[1]);
        } else {
            company.setFirstName(fullName);
            company.setLastName(null);
        }
        company.setEmail(editRequest.getEmail());
        company.setPhone(editRequest.getPhone());
        
        // Set mobile country code if provided
        if (editRequest.getMobileCountryCodeId() != null) {
            MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(editRequest.getMobileCountryCodeId())
                    .orElseThrow(() -> new RuntimeException("Mobile country code not found with ID: " + editRequest.getMobileCountryCodeId()));
            company.setMobileCountryCode(mobileCountryCode);
        }
        
        // Set branch entity if branchId is provided
        if (editRequest.getBranchId() != null) {
            Branch branch = branchRepository.findById(editRequest.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + editRequest.getBranchId()));
            company.setBranch(branch);
        } else {
            company.setBranch(null);
        }
        
        // Update CompanyDetails if exists
        CompanyDetails companyDetails = companyDetailsRepository.findByUserId(companyId)
                .orElse(new CompanyDetails());
        
        System.out.println("DEBUG: CompanyDetails before save - ID: " + companyDetails.getId() + ", Name: " + companyDetails.getCompanyName());
        
        companyDetails.setUser(company);
        companyDetails.setCompanyName(editRequest.getName());
        companyDetails.setContactPerson(editRequest.getContactPerson());
        companyDetails.setAddress(editRequest.getAddress());
        companyDetails.setWebsite(editRequest.getWebsite());
        companyDetails.setIndustry(editRequest.getIndustry());

        // Note: assignedTo should only be managed through hierarchy assignment API
        // Not during company update
        
        System.out.println("DEBUG: CompanyDetails after setting fields - ID: " + companyDetails.getId() + ", Name: " + companyDetails.getCompanyName());
        CompanyDetails savedCompanyDetails = companyDetailsRepository.save(companyDetails);
        System.out.println("DEBUG: CompanyDetails after save - ID: " + savedCompanyDetails.getId() + ", Name: " + savedCompanyDetails.getCompanyName());
        
        return userRepository.save(company);
    }

    public User toggleCompanyStatus(Long companyId) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new BusinessException("User is not a company");
        }
        
        // Toggle status between ACTIVE and INACTIVE
        if (company.getStatus() == com.lab.atlasmentor.enums.UserStatus.INACTIVE) {
            company.setStatus(com.lab.atlasmentor.enums.UserStatus.ACTIVE);
        } else {
            company.setStatus(com.lab.atlasmentor.enums.UserStatus.INACTIVE);
        }
        
        return userRepository.save(company);
    }

    public User deactivateCompany(Long companyId) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new BusinessException("User is not a company");
        }
        
        // Set status to INACTIVE
        company.setStatus(com.lab.atlasmentor.enums.UserStatus.INACTIVE);
        
        return userRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new BusinessException("User is not a company");
        }
        
        // Delete company details if exists
        companyDetailsRepository.findByUserId(companyId)
                .ifPresent(cd -> companyDetailsRepository.delete(cd));
        
        // No need to delete user roles since we're using enum approach
        
        // Delete user
        userRepository.delete(company);
    }
}
