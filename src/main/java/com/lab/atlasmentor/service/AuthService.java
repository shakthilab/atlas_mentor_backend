package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.AuthResponse;
import com.lab.atlasmentor.dto.EmployeeEditRequest;
import com.lab.atlasmentor.dto.EmployeeRequest;
import com.lab.atlasmentor.dto.EmployeeResponse;
import com.lab.atlasmentor.dto.LoginRequest;
import com.lab.atlasmentor.dto.RegisterRequest;
import com.lab.atlasmentor.enums.EmployeeType;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.EmployeeDetails;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.EmployeeDetailsRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.service.BranchService;
import com.lab.atlasmentor.service.RoleService;
import com.lab.atlasmentor.util.PasswordGenerator;
import com.lab.atlasmentor.util.SecurityUtil;
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

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private BranchService branchService;
    
    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;
    
    @Autowired
    private RoleCacheService roleCacheService;

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

        User savedUser = userRepository.save(user);
        
        // Assign STUDENT role to registered users
        roleService.assignRolesToUser(savedUser.getId(), "STUDENT");
        
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        
        return savedUser;
    }

    public User createEmployee(EmployeeRequest employeeRequest, HttpServletRequest request) {
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
        user.setBranchId(employeeRequest.getBranchId());
        user.setIsVerified(true); // Employees are pre-verified
        
        // Set createdBy from current admin user
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            User currentUser = securityUtil.extractUserFromToken(token);
            user.setCreatedBy(currentUser);
            user.setUpdatedBy(currentUser);
        }
        
        User savedUser = userRepository.save(user);
        
        // Assign specified role to new employee using roleId
        roleService.assignRoleToUserById(savedUser.getId(), employeeRequest.getRoleId());
        
        // Create EmployeeDetails record
        EmployeeDetails employeeDetails = new EmployeeDetails();
        employeeDetails.setUser(savedUser);
        
        // Determine employeeType from roleId
        EmployeeType employeeType = employeeRequest.getEmployeeType();
        if (employeeType == null) {
            Role role = roleCacheService.getRoleById(employeeRequest.getRoleId());
            String roleName = role.getName();
            
            // Map role names to employee types
            switch (roleName) {
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
            User currentUser = securityUtil.extractUserFromToken(token);
            employeeDetails.setCreatedBy(currentUser);
            employeeDetails.setUpdatedBy(currentUser);
        }
        
        employeeDetailsRepository.save(employeeDetails);
        
        // Send credentials email
        emailService.sendEmployeeCredentialsEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            generatedPassword
        );
        
        return savedUser;
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

        String userRole = roleService.getUserRole(user.getId());
        String primaryRole = userRole != null ? userRole : "USER";
        
        String token = jwtService.generateToken(user.getEmail(), user.getId(), primaryRole);

        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), primaryRole);
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

    public Page<EmployeeResponse> getAllEmployees(int page, int size, String role, Long branch, String search) {
        // Define all roles
        List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "VIDEO_EDITOR", "JUNIOR_COUNSELLOR", "SENIOR_COUNSELLOR", "COUNSELLOR");
        
        // Convert search to lowercase for case-insensitive search
        String searchLower = search != null ? search.toLowerCase() : null;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> employees = userRepository.findEmployeesWithFilters(role, branch, searchLower, employeeRoleNames, pageable);
        
        return employees.map(user -> {
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
            
            return new EmployeeResponse(
                user.getId(),
                user.getFullName(),
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
                user.getUpdatedBy()
            );
        });
    }
    
    @Transactional
    public User editEmployee(Long employeeId, EmployeeEditRequest editRequest) {
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        // Verify it's an employee (not a student or other role)
        List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "VIDEO_EDITOR", "SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR");
        boolean isEmployee = employeeRoleNames.contains(employee.getRole().getName());
        
        if (!isEmployee) {
            throw new RuntimeException("User is not an employee");
        }
        
        // Update editable fields (email is excluded)
        employee.setFirstName(editRequest.getFirstName());
        employee.setLastName(editRequest.getLastName());
        employee.setPhone(editRequest.getPhone());
        employee.setBranchId(editRequest.getBranchId());
        
        // Update role if different
        roleService.assignRoleToUserById(employee.getId(), editRequest.getRoleId());
        
        return userRepository.save(employee);
    }
    
    @Transactional
    public String updateEmployeeStatus(Long employeeId, String status) {
        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        // Verify it's an employee (not a student or other role)
        List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "EMPLOYEE", "VIDEO_EDITOR", "COUNSELLOR");
        boolean isEmployee = employeeRoleNames.contains(employee.getRole().getName());
        
        if (!isEmployee) {
            throw new RuntimeException("User is not an employee");
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
        
        // Verify it's an employee (not a student or other role)
        List<String> employeeRoleNames = List.of("ADMIN", "MANAGER", "EMPLOYEE", "VIDEO_EDITOR", "COUNSELLOR");
        boolean isEmployee = employeeRoleNames.contains(employee.getRole().getName());
        
        if (!isEmployee) {
            throw new RuntimeException("User is not an employee");
        }
        
        userRepository.delete(employee);
    }
}
