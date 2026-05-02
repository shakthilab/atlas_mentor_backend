package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.exception.EmailAlreadyExistsException;
import com.lab.atlasmentor.exception.UserNotFoundException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.util.PasswordGenerator;
import com.lab.atlasmentor.util.SecurityUtil;
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

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private ManagerEmployeeHierarchyRepository managerEmployeeHierarchyRepository;

    @Autowired
    private MobileCountryCodeRepository mobileCountryCodeRepository;

    public UserResponse createUser(CreateUserRequest request, String createdByRoleName) {
        // Validate role creation permissions
        validateRoleCreation(request.getRole(), createdByRoleName);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
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

    public List<UserResponse> getUsersExcludingAdminAndStudent(Long roleId, Long branchId) {
        List<String> excludedRoles = List.of("ADMIN", "STUDENT");
        List<User> users;
        
        if (roleId != null && branchId != null) {
            users = userRepository.findUsersExcludingRolesWithRoleIdAndBranchId(excludedRoles, roleId, branchId);
        } else if (roleId != null) {
            users = userRepository.findUsersExcludingRolesWithRoleId(excludedRoles, roleId);
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
        return getUsersExcludingAdminAndStudent(roleId, null);
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
                .filter(user -> user.hasRole("MANAGER"))
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

    public List<UnassignedEmployeeResponse> getUnassignedEmployees() {
        // Get all employee IDs that are assigned to managers
        List<Long> assignedEmployeeIds = managerEmployeeHierarchyRepository.findAll().stream()
                .map(ManagerEmployeeHierarchy::getEmployeeId)
                .collect(Collectors.toList());
        
        // Get all users with employee roles (excluding managers and senior counselors)
        List<String> employeeRoles = List.of(
            "JUNIOR_COUNSELLOR",
            "VIDEO_EDITOR"
        );
        
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == com.lab.atlasmentor.enums.UserStatus.ACTIVE)
                .filter(user -> !assignedEmployeeIds.contains(user.getId()))
                .filter(user -> user.getRole() != null && employeeRoles.contains(user.getRole().getName()))
                .map(this::convertToUnassignedEmployeeResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUser(Long userId, CreateUserRequest request, String createdByRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate role update permissions
        validateRoleCreation(request.getRole(), createdByRoleName);

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
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

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }

    private void validateRoleCreation(String roleToCreateName, String createdByRoleName) {
        // Admin can create all roles except another admin
        if ("ADMIN".equals(createdByRoleName) && "ADMIN".equals(roleToCreateName)) {
            throw new RuntimeException("Admin cannot create another admin user");
        }

        if ("ADMIN".equals(createdByRoleName)) {
            // Admin can create: MANAGER
            if ("STUDENT".equals(roleToCreateName)) {
                throw new RuntimeException("Students must register themselves");
            }
        } else if ("MANAGER".equals(createdByRoleName)) {
            // Manager can create: JUNIOR_COUNSELLOR, COUNSELLOR, VIDEO_EDITOR
            if (!List.of("JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "VIDEO_EDITOR").contains(roleToCreateName)) {
                throw new RuntimeException("Manager can only create JUNIOR_COUNSELLOR, SENIOR_COUNSELLOR, or VIDEO_EDITOR users");
            }
        } else {
            throw new RuntimeException("Only ADMIN and MANAGER can create users");
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
            currentUser = securityUtil.extractUserFromToken(token);
            user.setCreatedBy(currentUser);
            user.setUpdatedBy(currentUser);
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
                referralDetails.setCreatedBy(currentUser);
                referralDetails.setUpdatedBy(currentUser);
            }
            
            System.out.println("Step 2: ReferralDetails created - userId: " + referralDetails.getUserId() + ", type: " + referralDetails.getReferralType());
            System.out.println("Step 3: About to save ReferralDetails");
            ReferralDetails savedReferralDetails = referralDetailsRepository.save(referralDetails);
            System.out.println("Step 4: ReferralDetails saved successfully with ID: " + savedReferralDetails.getId());
            System.out.println("Step 5: ReferralDetails saved - userId: " + savedReferralDetails.getUserId() + ", type: " + savedReferralDetails.getReferralType());
        } catch (Exception e) {
            System.err.println("ERROR in ReferralDetails save process:");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new RuntimeException("Failed to save referral details: " + e.getMessage());
        }
        
        // Send credentials email
        emailService.sendEmployeeCredentialsEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            generatedPassword
        );
        
        return savedUser;
    }

    public User createCompany(CompanyRequest companyRequest, HttpServletRequest request) {
        if (userRepository.existsByEmail(companyRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
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
            User currentUser = securityUtil.extractUserFromToken(token);
            user.setCreatedBy(currentUser);
            user.setUpdatedBy(currentUser);
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
            User currentUser = securityUtil.extractUserFromToken(token);
            companyDetails.setCreatedBy(currentUser);
            companyDetails.setUpdatedBy(currentUser);
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

    public Page<UserResponse> getReferrals(int page, int size, String search, String referralType, Long branchId) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Get user IDs filtered by referral type if provided
        List<Long> filteredUserIds = null;
        if (referralType != null && !referralType.isEmpty()) {
            try {
                com.lab.atlasmentor.enums.ReferralType type = com.lab.atlasmentor.enums.ReferralType.valueOf(referralType.toUpperCase());
                filteredUserIds = referralDetailsRepository.findUserIdsByReferralType(type);
            } catch (IllegalArgumentException e) {
                return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
            }
        }
        
        // Create effectively final copy for lambda expression
        final List<Long> finalFilteredUserIds = filteredUserIds;
        
        // Get users with REFERRAL role
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
        
        return referrals.map(this::convertToUserResponse);
    }

    public Page<UserResponse> getCompanies(int page, int size, String search, Long branchId) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Get users with COMPANY role
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
        
        return companies.map(this::convertToUserResponse);
    }

    public User getCompanyById(Long companyId) {
        // Check if user exists and has COMPANY role
        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        
        if (!company.hasRole("COMPANY")) {
            throw new RuntimeException("User is not a company");
        }
        
        return company;
    }

    public UserResponse convertToUserResponse(User user) {
        Role primaryRole = user.getRole();
        
        // Get referral type and assignedTo for users with REFERRAL role
        com.lab.atlasmentor.enums.ReferralType referralType = null;
        String assignedToUsername = null;
        if (primaryRole != null && "REFERRAL".equals(primaryRole.getName())) {
            referralType = referralDetailsRepository.findByUserId(user.getId())
                    .map(ReferralDetails::getReferralType)
                    .orElse(null);
            
            assignedToUsername = referralDetailsRepository.findByUserId(user.getId())
                    .map(referralDetails -> referralDetails.getAssignedTo() != null ? 
                            referralDetails.getAssignedTo().getFullName() : null)
                    .orElse(null);
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
        
        // Get staff and student counts for companies
        UserResponse.UserCounts userCounts = null;
        if (primaryRole != null && "COMPANY".equals(primaryRole.getName()) && user.getBranchId() != null) {
            // Define staff roles (excluding ADMIN as they don't belong to branches)
            List<String> staffRoles = List.of("MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR");
            
            Long totalStaffs = userRepository.countStaffsByBranchId(user.getBranchId(), staffRoles);
            Long totalStudents = userRepository.countStudentsByBranchId(user.getBranchId());
            
            userCounts = new UserResponse.UserCounts(totalStaffs, totalStudents);
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
                assignedToUsername,
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
        // Check if user exists and has REFERRAL role
        User referral = userRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));
        
        if (!referral.hasRole("REFERRAL")) {
            throw new RuntimeException("User is not a referral");
        }
        
        // Delete referral details first (if exists)
        referralDetailsRepository.findByUserId(referralId)
                .ifPresent(referralDetails -> referralDetailsRepository.delete(referralDetails));
        
        // No need to delete user roles since we're using enum approach
        
        // Delete user
        userRepository.delete(referral);
    }

    public User updateReferral(Long referralId, ReferralRequest request) {
        // Check if user exists and has REFERRAL role
        User referral = userRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));
        
        if (!referral.hasRole("REFERRAL")) {
            throw new RuntimeException("User is not a referral");
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
            throw new RuntimeException("User is not a referral");
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
            throw new RuntimeException("User is not a company");
        }
        
        // Check if email is being changed and if new email already exists
        if (!company.getEmail().equals(editRequest.getEmail()) && 
            userRepository.existsByEmail(editRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
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
            throw new RuntimeException("User is not a company");
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
            throw new RuntimeException("User is not a company");
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
            throw new RuntimeException("User is not a company");
        }
        
        // Delete company details if exists
        companyDetailsRepository.findByUserId(companyId)
                .ifPresent(cd -> companyDetailsRepository.delete(cd));
        
        // No need to delete user roles since we're using enum approach
        
        // Delete user
        userRepository.delete(company);
    }
}
