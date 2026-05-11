package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.AuthResponse;
import com.lab.atlasmentor.dto.EmployeeEditRequest;
import com.lab.atlasmentor.dto.EmployeeRequest;
import com.lab.atlasmentor.dto.EmployeeResponse;
import com.lab.atlasmentor.dto.LoginRequest;
import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.RegisterRequest;
import com.lab.atlasmentor.enums.EmployeeType;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.EmployeeDetails;
import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.CounsellorHierarchyRepository;
import com.lab.atlasmentor.repository.EmployeeDetailsRepository;
import com.lab.atlasmentor.repository.MobileCountryCodeRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import com.lab.atlasmentor.service.BranchCacheService;
import com.lab.atlasmentor.service.BranchService;
import com.lab.atlasmentor.service.RoleService;
import com.lab.atlasmentor.util.PasswordGenerator;
import com.lab.atlasmentor.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RoleService roleService;

    // SecurityUtil removed - now using SecurityUtils directly

    @Autowired
    private BranchService branchService;
    
    @Autowired
    private BranchCacheService branchCacheService;
    
    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;
    
    @Autowired
    private RoleCacheService roleCacheService;
    
    @Autowired
    private MobileCountryCodeRepository mobileCountryCodeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CounsellorHierarchyRepository counsellorHierarchyRepository;

    public User register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhone(registerRequest.getPhone());
        user.setIsVerified(false);
        
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));

        // Assign STUDENT role to registered users before saving
        Role studentRole = roleService.findRole("STUDENT");
        user.setRole(studentRole);

        User savedUser = userRepository.save(user);
        
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        
        return savedUser;
    }

    public EmployeeResponse createEmployee(EmployeeRequest employeeRequest, HttpServletRequest request) {
        if (userRepository.existsByEmail(employeeRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Generate random password
        String generatedPassword = PasswordGenerator.generateRandomPassword();

        User user = new User();
        user.setFirstName(employeeRequest.getFirstName());
        user.setLastName(employeeRequest.getLastName());
        user.setEmail(employeeRequest.getEmail());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setPhone(employeeRequest.getPhone());
        user.setIsVerified(true);
        
        // Set branch if provided
        if (employeeRequest.getBranchId() != null) {
            Branch branch = branchCacheService.getBranchById(employeeRequest.getBranchId());
            user.setBranch(branch);
        }
        
        // Set mobile country code if provided
        if (employeeRequest.getMobileCountryCodeId() != null) {
            MobileCountryCode mcc = mobileCountryCodeRepository.findById(employeeRequest.getMobileCountryCodeId())
                .orElseThrow(() -> new RuntimeException("Mobile country code not found with id: " + employeeRequest.getMobileCountryCodeId()));
            user.setMobileCountryCode(mcc);
        }
        
        // Set role before saving to satisfy NOT NULL constraint
        Role role = roleCacheService.getRoleById(employeeRequest.getRoleId());
        user.setRole(role);
        
        // Set createdBy from current admin user
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            user.setCreatedBy(currentUser.getId());
            user.setUpdatedBy(currentUser.getId());
        }
        
        User savedUser = userRepository.save(user);
        
        // Create EmployeeDetails record
        EmployeeDetails employeeDetails = new EmployeeDetails();
        employeeDetails.setUser(savedUser);
        
        // Determine employeeType from roleId
        EmployeeType employeeType = employeeRequest.getEmployeeType();
        if (employeeType == null) {
            String roleName = role.getName();
            
            // Map role names to employee types
            switch (roleName) {
                case "ADMIN":
                    employeeType = EmployeeType.ADMIN;
                    break;
                case "MANAGER":
                    employeeType = EmployeeType.MANAGER;
                    break;
                case "JUNIOR_COUNSELLOR":
                    employeeType = EmployeeType.JUNIOR_COUNSELLOR;
                    break;
                case "SENIOR_COUNSELLOR":
                    employeeType = EmployeeType.SENIOR_COUNSELLOR;
                    break;
                case "VIDEO_EDITOR":
                    employeeType = EmployeeType.VIDEO_EDITOR;
                    break;
                case "GRAPHIC_DESIGNER":
                    employeeType = EmployeeType.GRAPHIC_DESIGNER;
                    break;
                case "WEB_DEV":
                    employeeType = EmployeeType.WEB_DEV;
                    break;
                default:
                    employeeType = EmployeeType.JUNIOR_COUNSELLOR;
                    break;
            }
        }
        employeeDetails.setEmployeeType(employeeType);
        
        // Set manager - can be null for top-level employees
        Long managerId = employeeRequest.getManagerId();
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found with id: " + managerId));
            employeeDetails.setManager(manager);
        }
        
        // Set isSenior to true for SENIOR_COUNSELLOR type
        if (employeeType == com.lab.atlasmentor.enums.EmployeeType.SENIOR_COUNSELLOR) {
            employeeDetails.setIsSenior(true);
        } else {
            employeeDetails.setIsSenior(employeeRequest.getIsSenior() != null ? employeeRequest.getIsSenior() : false);
        }
        
        // Set createdBy and updatedBy from current admin user
        if (token != null && token.startsWith("Bearer ")) {
            User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
            employeeDetails.setCreatedBy(currentUser.getId());
            employeeDetails.setUpdatedBy(currentUser.getId());
        }
        
        employeeDetailsRepository.save(employeeDetails);
        
        // Send credentials email
        emailService.sendEmployeeCredentialsEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            generatedPassword
        );
        
        // Convert to EmployeeResponse to avoid lazy loading issues
        return convertToEmployeeResponse(savedUser);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.getIsVerified()) {
            throw new RuntimeException("Please verify your email before logging in");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Your account is not active. Please contact administrator.");
        }

        String primaryRole = user.getRole() != null ? user.getRole().getName() : "USER";
        
        // Check if the role is marked as employee
        boolean isEmployee = user.getRole() != null && user.getRole().getIsEmployee();
        
        String token = jwtService.generateToken(user.getEmail(), user.getId(), primaryRole, user.getBranchId());

        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), primaryRole, isEmployee);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        userRepository.verifyUser(user.getId());
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));

        userRepository.save(user);
        
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));

        userRepository.save(user);
        
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        userRepository.resetPassword(user.getId(), encodedPassword);
    }

    public PageResponse<EmployeeResponse> getAllEmployees(int page, int size, String role, Long branch, String search) {
        try {
            // Get current user for branch-based filtering
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Getting employees with branch-based access control: userId={}, role={}, branchId={}, isAdmin={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            
            // Apply branch-based filtering for non-admin users
            Long effectiveBranch = branch;
            if (!currentUser.isAdmin() && branch == null) {
                effectiveBranch = currentUser.getBranchId();
            }
            
            // Define all roles
            List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR");
            
            // Convert search to lowercase for case-insensitive search
            String searchLower = search != null ? search.toLowerCase() : null;
            
            Pageable pageable = PageRequest.of(page, size);
            Page<User> employees = userRepository.findEmployeesWithFilters(role, effectiveBranch, searchLower, employeeRoleNames, pageable);
        
        // Convert Page<User> to List<EmployeeResponse>
        List<EmployeeResponse> employeeResponses = employees.getContent().stream().map(user -> {
            List<EmployeeResponse.RoleDto> roleDtos = List.of(
                new EmployeeResponse.RoleDto(user.getRole().getId(), user.getRole().getName(), user.getRole().getDescription())
            );
            
            // Create branch DTO if branchId exists
            EmployeeResponse.BranchDto branchDto = user.getBranchId() != null 
                ? branchService.getBranchById(user.getBranchId())
                    .map(branchEntity -> new EmployeeResponse.BranchDto(
                        branchEntity.getId(),
                        branchEntity.getName(),
                        branchEntity.getLocation(),
                        branchEntity.getStatus().name()
                    ))
                    .orElse(null)
                : null;
            
            // Create mobile country code DTO if exists
            EmployeeResponse.MobileCountryCodeDto mccDto = user.getMobileCountryCode() != null
                ? new EmployeeResponse.MobileCountryCodeDto(
                    user.getMobileCountryCode().getId(),
                    user.getMobileCountryCode().getCountryName(),
                    user.getMobileCountryCode().getCountryCode(),
                    user.getMobileCountryCode().getMobileCode(),
                    user.getMobileCountryCode().getIsoAlpha2(),
                    user.getMobileCountryCode().getIsoAlpha3(),
                    user.getMobileCountryCode().getIsActive(),
                    user.getMobileCountryCode().getFlagUrl(),
                    user.getMobileCountryCode().getMobileNumberLength()
                )
                : null;
            
            // Get task counts for the employee
            Long pendingTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.PENDING);
            Long inProgressTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.IN_PROGRESS);
            Long completedTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.COMPLETED);
            
            EmployeeResponse.TaskCount taskCount = new EmployeeResponse.TaskCount(
                pendingTaskCount, inProgressTaskCount, completedTaskCount
            );
            
            return new EmployeeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getBranchId(),
                branchDto,
                user.getStatus().name(),
                user.getIsVerified(),
                roleDtos.get(0),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy(),
                mccDto,
                taskCount
            );
        }).collect(Collectors.toList());
        
        // Create and return PageResponse
        return PageResponse.of(
            employeeResponses,
            employees.getNumber(),
            employees.getSize(),
            employees.getTotalElements()
        );
        } catch (Exception e) {
            log.error("Error getting current user for employee filtering: {}", e.getMessage(), e);
            // Fallback to original behavior without branch filtering
            List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR", "BRANCH_PARTNER");
            String searchLower = search != null ? search.toLowerCase() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<User> employees = userRepository.findEmployeesWithFilters(role, branch, searchLower, employeeRoleNames, pageable);
            
            // Convert to EmployeeResponse list
            List<EmployeeResponse> employeeResponses = employees.getContent().stream()
                .map(user -> {
                    // Simplified mapping for fallback
                    List<EmployeeResponse.RoleDto> roleDtos = List.of(
                        new EmployeeResponse.RoleDto(user.getRole().getId(), user.getRole().getName(), user.getRole().getDescription())
                    );
                    return new EmployeeResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getBranchId(),
                        null, // branchDto
                        user.getStatus().name(),
                        user.getIsVerified(),
                        roleDtos.get(0),
                        user.getCreatedAt(),
                        user.getUpdatedAt(),
                        user.getCreatedBy(),
                        user.getUpdatedBy(),
                        null, // mccDto
                        null  // taskCount
                    );
                })
                .collect(Collectors.toList());
            
            // Return PageResponse for fallback
            return PageResponse.of(
                employeeResponses,
                employees.getNumber(),
                employees.getSize(),
                employees.getTotalElements()
            );
        }
    }
    
    @Transactional
    public EmployeeResponse editEmployee(Long employeeId, EmployeeEditRequest editRequest) {
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        // Verify user is not a student (students cannot be edited via employee endpoint)
        if (employee.getRole() != null && "STUDENT".equals(employee.getRole().getName())) {
            throw new RuntimeException("Students cannot be edited via employee endpoint");
        }
        
        // Update editable fields (email is excluded)
        employee.setFirstName(editRequest.getFirstName());
        employee.setLastName(editRequest.getLastName());
        employee.setPhone(editRequest.getPhone());
        
        // Set branch if provided
        if (editRequest.getBranchId() != null) {
            Branch branch = branchCacheService.getBranchById(editRequest.getBranchId());
            employee.setBranch(branch);
        } else {
            employee.setBranch(null);
        }
        
        // Update role if different
        roleService.assignRoleToUserById(employee.getId(), editRequest.getRoleId());
        
        User savedEmployee = userRepository.save(employee);
        
        // Convert to EmployeeResponse to avoid lazy loading issues
        return convertToEmployeeResponse(savedEmployee);
    }
    
    private EmployeeResponse convertToEmployeeResponse(User user) {
        // Create role DTO
        EmployeeResponse.RoleDto roleDto = new EmployeeResponse.RoleDto(
            user.getRole().getId(), 
            user.getRole().getName(), 
            user.getRole().getDescription()
        );
        
        // Create branch DTO if branchId exists
        EmployeeResponse.BranchDto branchDto = user.getBranchId() != null 
            ? branchService.getBranchById(user.getBranchId())
                .map(branchEntity -> new EmployeeResponse.BranchDto(
                    branchEntity.getId(),
                    branchEntity.getName(),
                    branchEntity.getLocation(),
                    branchEntity.getStatus().name()
                ))
                .orElse(null)
            : null;
        
        // Create mobile country code DTO if exists
        EmployeeResponse.MobileCountryCodeDto mccDto = user.getMobileCountryCode() != null
            ? new EmployeeResponse.MobileCountryCodeDto(
                user.getMobileCountryCode().getId(),
                user.getMobileCountryCode().getCountryName(),
                user.getMobileCountryCode().getCountryCode(),
                user.getMobileCountryCode().getMobileCode(),
                user.getMobileCountryCode().getIsoAlpha2(),
                user.getMobileCountryCode().getIsoAlpha3(),
                user.getMobileCountryCode().getIsActive(),
                user.getMobileCountryCode().getFlagUrl(),
                user.getMobileCountryCode().getMobileNumberLength()
            )
            : null;
        
        // Get task counts for the employee
        Long pendingTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.PENDING);
        Long inProgressTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.IN_PROGRESS);
        Long completedTaskCount = taskRepository.countByAssignedToIdAndStatus(user.getId(), TaskStatus.COMPLETED);
        
        EmployeeResponse.TaskCount taskCount = new EmployeeResponse.TaskCount(
            pendingTaskCount, inProgressTaskCount, completedTaskCount
        );
        
        return new EmployeeResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhone(),
            user.getBranchId(),
            branchDto,
            user.getStatus().name(),
            user.getIsVerified(),
            roleDto,
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getCreatedBy(),
            user.getUpdatedBy(),
            mccDto,
            taskCount
        );
    }
    
    @Transactional
    public String updateEmployeeStatus(Long employeeId, String status) {
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        // Verify user is not a student (student status cannot be updated via employee endpoint)
        if (employee.getRole() != null && "STUDENT".equals(employee.getRole().getName())) {
            throw new RuntimeException("Students cannot be updated via employee endpoint");
        }
        
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
        UserStatus currentStatus = employee.getStatus();
        
        userRepository.updateUserStatus(employeeId, newStatus);
        
        if (currentStatus == newStatus) {
            return "already_" + status.toLowerCase();
        } else {
            return status.toLowerCase();
        }
    }
    
    @Transactional
    public void deleteEmployee(Long employeeId) {
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        // Verify user is not a student (students cannot be deleted via employee endpoint)
        if (employee.getRole() != null && "STUDENT".equals(employee.getRole().getName())) {
            throw new RuntimeException("Students cannot be deleted via employee endpoint");
        }
        
        // Delete counsellor hierarchy entries where employee is senior or junior
        counsellorHierarchyRepository.deleteBySeniorCounsellor_Id(employeeId);
        counsellorHierarchyRepository.deleteByJuniorCounsellor_Id(employeeId);
        
        // Delete employee_details entries where employee is user, manager, or assignedTo
        employeeDetailsRepository.deleteByUser_Id(employeeId);
        employeeDetailsRepository.deleteByManager_Id(employeeId);
        employeeDetailsRepository.deleteByAssignedTo_Id(employeeId);
        
        userRepository.delete(employee);
    }
}
