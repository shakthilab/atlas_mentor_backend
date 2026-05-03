package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.dto.StudentOnboardingRequest;
import com.lab.atlasmentor.dto.StudentStatusUpdateRequest;
import com.lab.atlasmentor.dto.StudentResponse;
import com.lab.atlasmentor.dto.StudentWithStudentPaymentDto;
import com.lab.atlasmentor.dto.StudentPaymentAmountUpdateRequest;
import com.lab.atlasmentor.dto.StudentPaymentStatusUpdateRequest;
import com.lab.atlasmentor.dto.StudentPaymentAmountDto;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.service.RoleCacheService;
import com.lab.atlasmentor.service.FinalPaymentService;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.enums.SourceType;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import com.lab.atlasmentor.enums.ApprovalStatus;
import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentNoteRepository studentNoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MobileCountryCodeRepository mobileCountryCodeRepository;

    @Autowired
    private StudentAcademicHistoryRepository studentAcademicHistoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StudentActivityRepository studentActivityRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleCacheService roleCacheService;

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Autowired
    private FinalPaymentService finalPaymentService;

    @Autowired
    private DisputeRepository disputeRepository;

    @Transactional
    public Student registerStudent(StudentRegistrationRequest request) {
        // First register the user account
        var registerRequest = new com.lab.atlasmentor.dto.RegisterRequest();
        registerRequest.setFirstName(request.getFirstName());
        registerRequest.setLastName(request.getLastName());
        registerRequest.setEmail(request.getEmail());
        registerRequest.setPhone(request.getPhone());
        registerRequest.setPassword(request.getPassword());
        
        var user = authService.register(registerRequest);
        
        // Create student record
        Student student = new Student();
        student.setUser(user);
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setStatus(StudentStatus.LEAD);
        
        // Branch is optional for students - leave as null for now
        
        student.setCreatedBy(user.getId());
        
        // Set mobile country code if provided
        if (request.getMobileCountryCodeId() != null) {
            MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(request.getMobileCountryCodeId())
                .orElseThrow(() -> new RuntimeException("Mobile country code not found with id: " + request.getMobileCountryCodeId()));
            student.setMobileCountryCode(mobileCountryCode);
        }
        
        // Set country and university if provided
        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found with id: " + request.getCountryId()));
            student.setCountry(country);
        }
        
        if (request.getUniversityId() != null) {
            University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found with id: " + request.getUniversityId()));
            student.setUniversity(university);
        }
        
        // Set notes if provided
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            student.setNotes(request.getNotes());
        }
        
        Student savedStudent = studentRepository.save(student);
        
        // Create student note with academic preferences and background
        if (hasAcademicData(request)) {
            StudentNote note = createStudentNote(savedStudent.getId(), request);
            studentNoteRepository.save(note);
        }
        
        return savedStudent;
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public Student getStudentByIdAsResponse(Long id) {
        Student student = getStudentById(id);
        
        // Populate branch fields for API convenience
        if (student.getBranch() != null) {
            student.setBranchId(student.getBranch().getId());
            student.setBranchName(student.getBranch().getName());
        }
        
        return student;
    }

    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        
        // The cascade operations will automatically delete:
        // - StudentAcademicHistory records (cascade = CascadeType.ALL)
        // - Document records (cascade = CascadeType.ALL)
        // - Any other related entities with cascade operations
        
        studentRepository.delete(student);
    }

    public PageResponse<Student> getAllStudents(StudentStatus status, String search, Pageable pageable) {
        var currentUser = SecurityUtils.getCurrentUser();
        
        String statusParam = (status != null) ? status.toString() : null;
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase() + "%" : "%";
        
        Page<Student> students;
        
        // Check user role and apply appropriate filtering
        String userRole = currentUser.getRole();
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Show all branch students
            students = studentRepository.findByFiltersWithAccess(
                statusParam, 
                searchParam, 
                true, 
                null, 
                pageable
            );
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            // Manager: Show only branch-specific students
            students = studentRepository.findByFiltersWithAccess(
                statusParam, 
                searchParam, 
                false, 
                currentUser.getBranchId(), 
                pageable
            );
        } else if ("JUNIOR_COUNSELLOR".equalsIgnoreCase(userRole) || "SENIOR_COUNSELLOR".equalsIgnoreCase(userRole)) {
            // Counsellors: Show only assigned students to them
            students = studentRepository.findByFiltersForCounsellor(
                statusParam, 
                searchParam, 
                currentUser.getUserId(), 
                pageable
            );
        } else if ("REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole)) {
            // Referral and Company: Show only students they created
            students = studentRepository.findByFiltersForCreator(
                statusParam, 
                searchParam, 
                currentUser.getUserId(), 
                pageable
            );
        } else {
            // Default: Apply branch-based access control (for other roles)
            students = studentRepository.findByFiltersWithAccess(
                statusParam, 
                searchParam, 
                currentUser.isAdmin(), 
                currentUser.getBranchId(), 
                pageable
            );
        }
        
        // Populate branch fields for each student
        students.getContent().forEach(student -> {
            if (student.getBranch() != null) {
                student.setBranchId(student.getBranch().getId());
                student.setBranchName(student.getBranch().getName());
            }
        });
        
        return PageResponse.of(students.getContent(), students.getNumber(), students.getSize(), students.getTotalElements());
    }

    public PageResponse<Student> getNonRegisteredStudents(String search, Pageable pageable) {
        var currentUser = SecurityUtils.getCurrentUser();
        
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase() + "%" : "%";
        
        Page<Student> students;
        
        // Check user role and apply appropriate filtering
        String userRole = currentUser.getRole();
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Show all branch students except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                true, 
                null, 
                pageable
            );
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            // Manager: Show only branch-specific students except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                false, 
                currentUser.getBranchId(), 
                pageable
            );
        } else if ("JUNIOR_COUNSELLOR".equalsIgnoreCase(userRole) || "SENIOR_COUNSELLOR".equalsIgnoreCase(userRole)) {
            // Counsellors: Show only assigned students to them except REGISTERED
            students = studentRepository.findByNonRegisteredStatusForCounsellor(
                searchParam, 
                currentUser.getUserId(), 
                pageable
            );
        } else if ("REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole)) {
            // Referral and Company: Show only students they created except REGISTERED
            students = studentRepository.findByNonRegisteredStatusForCreator(
                searchParam, 
                currentUser.getUserId(), 
                pageable
            );
        } else {
            // Default: Apply branch-based access control (for other roles) except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                currentUser.isAdmin(), 
                currentUser.getBranchId(), 
                pageable
            );
        }
        
        // Populate branch fields for each student
        students.getContent().forEach(student -> {
            if (student.getBranch() != null) {
                student.setBranchId(student.getBranch().getId());
                student.setBranchName(student.getBranch().getName());
            }
        });
        
        return PageResponse.of(students.getContent(), students.getNumber(), students.getSize(), students.getTotalElements());
    }




    private boolean hasAcademicData(StudentRegistrationRequest request) {

        return (request.getCountryId() != null) ||
               (request.getUniversityId() != null) ||
               (request.getCourse() != null && !request.getCourse().trim().isEmpty()) ||
               (request.getIntake() != null && !request.getIntake().trim().isEmpty()) ||
               (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) ||
               (request.getBasicAcademicDetails() != null && !request.getBasicAcademicDetails().trim().isEmpty()) ||
               (request.getOptionalNotes() != null && !request.getOptionalNotes().trim().isEmpty());
    }

    private StudentNote createStudentNote(Long studentId, StudentRegistrationRequest request) {
        StudentNote note = new StudentNote();
        note.setStudentId(studentId);
        // Get the student to find who created it
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        note.setCreatedBy(student.getCreatedBy());
        
        // Set individual fields directly
        note.setCourse(request.getCourse());
        note.setIntake(request.getIntake());
        note.setReferralCode(request.getReferralCode());
        note.setAcademicDetails(request.getBasicAcademicDetails());
        note.setAdditionalNotes(request.getOptionalNotes());
        
        return note;
    }

    public Student findStudentByEmail(String email) {
        return studentRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public Student createOrUpdateStudent(StudentOnboardingRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        User currentUser = userRepository.findById(currentUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Check if student already exists with this email
        Student existingStudent = studentRepository.findByEmail(request.getEmail()).orElse(null);
        
        if (existingStudent != null) {
            // Update existing student
            return updateStudentData(existingStudent, request, currentUser);
        } else {
            // Create new student
            return createNewStudent(request, currentUser, currentUserDetails);
        }
    }

    @Transactional
    public Student updateStudent(Long id, StudentOnboardingRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        User currentUser = userRepository.findById(currentUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        return updateStudentData(student, request, currentUser);
    }

    private Student createNewStudent(StudentOnboardingRequest request, User currentUser, CustomUserDetails currentUserDetails) {
        // Generate random password
        String generatedPassword = generateRandomPassword();
        
        // Create user account
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setCreatedBy(currentUser.getId());
        
        // Set role - fetch STUDENT role from cache
        Role studentRole = roleCacheService.getRoleByName("STUDENT");
        user.setRole(studentRole);
        
        user = userRepository.save(user);
        
        // Create student record
        Student student = new Student();
        student.setUser(user);
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setStatus(StudentStatus.LEAD);
        student.setCreatedBy(currentUser.getId());
        
        // Set optional fields
        if (request.getMobileCountryCodeId() != null) {
            MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(request.getMobileCountryCodeId())
                .orElseThrow(() -> new RuntimeException("Mobile country code not found"));
            student.setMobileCountryCode(mobileCountryCode);
        }
        
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));
            student.setBranch(branch);
        }
        
        if (request.getDestinationCountryId() != null) {
            Country country = countryRepository.findById(request.getDestinationCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
            student.setCountry(country);
        }
        
        if (request.getTargetUniversityId() != null) {
            University university = universityRepository.findById(request.getTargetUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found"));
            student.setUniversity(university);
        }
        
        student.setNotes(request.getNotes());
        student.setCourseName(request.getCourseName());
        student.setIntakePeriod(request.getIntakePeriod());
        
        // Save student first
        student = studentRepository.save(student);
        
        // Save academic history
        if (request.getAcademicHistory() != null && !request.getAcademicHistory().isEmpty()) {
            for (StudentOnboardingRequest.AcademicHistory history : request.getAcademicHistory()) {
                StudentAcademicHistory academicHistory = new StudentAcademicHistory();
                academicHistory.setStudent(student);
                academicHistory.setQualification(history.getLevel());
                academicHistory.setInstitutionName(history.getInstitutionName());
                if (history.getPassingYear() != null && !history.getPassingYear().trim().isEmpty()) {
                    try {
                        academicHistory.setPassingYear(Integer.parseInt(history.getPassingYear()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid passing year format: " + history.getPassingYear());
                    }
                }
                academicHistory.setScore(history.getScoreCgpa());
                academicHistory.setCreatedBy(currentUser.getId());
                studentAcademicHistoryRepository.save(academicHistory);
            }
        }
        
        // Save documents
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getDocuments().entrySet()) {
                Document document = new Document();
                document.setStudent(student);
                document.setDocumentType(entry.getKey());
                document.setBase64Content(entry.getValue());
                document.setCreatedBy(currentUser.getId());
                documentRepository.save(document);
            }
        }
        
        // Auto-create payment record for REFERRAL and COMPANY roles
        String userRole = currentUserDetails.getRole();
        if ("REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole)) {
            try {
                SourceType sourceType = "REFERRAL".equalsIgnoreCase(userRole) ? SourceType.REFERRAL : SourceType.COMPANY;
                Long sourceId = currentUserDetails.getUserId();
                
                // Create payment record
                finalPaymentService.createStudentPayment(student.getId(), sourceType, sourceId);
                
                // Update student with source information
                student.setSourceType(sourceType);
                student.setSourceId(sourceId);
                studentRepository.save(student);
                
            } catch (Exception e) {
                // Log error but don't fail the student creation
                System.err.println("Failed to create payment record for student: " + e.getMessage());
            }
        }
        
        // Send login credentials via email
        try {
            emailService.sendLoginCredentials(request.getEmail(), generatedPassword);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send login credentials: " + e.getMessage());
        }
        
        return student;
    }

    private Student updateStudentData(Student student, StudentOnboardingRequest request, User currentUser) {
        // Update user information if user exists
        if (student.getUser() != null) {
            User user = student.getUser();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhone(request.getPhone());
            userRepository.save(user);
        }
        
        // Update student information
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        
        if (request.getMobileCountryCodeId() != null) {
            MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(request.getMobileCountryCodeId())
                .orElseThrow(() -> new RuntimeException("Mobile country code not found"));
            student.setMobileCountryCode(mobileCountryCode);
        }
        
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));
            student.setBranch(branch);
        }
        
        if (request.getDestinationCountryId() != null) {
            Country country = countryRepository.findById(request.getDestinationCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
            student.setCountry(country);
        }
        
        if (request.getTargetUniversityId() != null) {
            University university = universityRepository.findById(request.getTargetUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found"));
            student.setUniversity(university);
        }
        
        student.setNotes(request.getNotes());
        student.setCourseName(request.getCourseName());
        student.setIntakePeriod(request.getIntakePeriod());
        
        // Update academic history - remove existing and add new
        studentAcademicHistoryRepository.deleteByStudentId(student.getId());
        if (request.getAcademicHistory() != null && !request.getAcademicHistory().isEmpty()) {
            for (StudentOnboardingRequest.AcademicHistory history : request.getAcademicHistory()) {
                StudentAcademicHistory academicHistory = new StudentAcademicHistory();
                academicHistory.setStudent(student);
                academicHistory.setQualification(history.getLevel());
                academicHistory.setInstitutionName(history.getInstitutionName());
                if (history.getPassingYear() != null && !history.getPassingYear().trim().isEmpty()) {
                    try {
                        academicHistory.setPassingYear(Integer.parseInt(history.getPassingYear()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid passing year format: " + history.getPassingYear());
                    }
                }
                academicHistory.setScore(history.getScoreCgpa());
                academicHistory.setCreatedBy(currentUser.getId());
                studentAcademicHistoryRepository.save(academicHistory);
            }
        }
        
        // Update documents - remove existing and add new
        documentRepository.deleteByStudentId(student.getId());
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getDocuments().entrySet()) {
                Document document = new Document();
                document.setStudent(student);
                document.setDocumentType(entry.getKey());
                document.setBase64Content(entry.getValue());
                document.setCreatedBy(currentUser.getId());
                documentRepository.save(document);
            }
        }
        
        return studentRepository.save(student);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    public java.util.Map<String, java.util.List<String>> getRequiredDocuments() {
        java.util.Map<String, java.util.List<String>> requiredDocuments = new java.util.HashMap<>();
        
        // Core required documents for all students
        java.util.List<String> coreDocuments = java.util.Arrays.asList(
            "passport",
            "10th_marksheet", 
            "12th_marksheet",
            "birth_certificate"
        );
        
        // Additional documents based on requirements
        java.util.List<String> additionalDocuments = java.util.Arrays.asList(
            "police_clearance",
            "bank_statement",
            "insurance_document",
            "neet_scorecard"
        );
        
        requiredDocuments.put("required", coreDocuments);
        requiredDocuments.put("additional", additionalDocuments);
        
        return requiredDocuments;
    }
    
    @Transactional
    public Student updateStudentStatus(Long studentId, StudentStatusUpdateRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        User currentUser = userRepository.findById(currentUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        StudentStatus oldStatus = student.getStatus();
        StudentStatus newStatus = request.getStatus();
        
        // Only update if status is actually changing
        if (!oldStatus.equals(newStatus)) {
            // Update student status
            student.setStatus(newStatus);
            student.setUpdatedBy(currentUser.getId());
            
            // Create activity log
            String description = String.format("Student status changed from %s to %s", oldStatus, newStatus);
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                description += " - " + request.getNotes();
            }
            
            StudentActivity activity = new StudentActivity(
                student, 
                newStatus, 
                oldStatus.toString(), 
                newStatus.toString(), 
                description, 
                currentUser
            );
            
            studentActivityRepository.save(activity);
        }
        
        return studentRepository.save(student);
    }
    
    public java.util.List<StudentActivity> getStudentActivities(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        return studentActivityRepository.findByStudentOrderByPerformedAtDesc(student);
    }
    
    @Transactional
    public StudentPayment updateStudentPaymentAmount(StudentPaymentAmountUpdateRequest request) {
        StudentPayment payment = studentPaymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Student payment not found with id: " + request.getPaymentId()));
        
        // Check if amount is locked
        if (payment.getIsAmountLocked()) {
            throw new RuntimeException("Payment amount is locked and cannot be modified");
        }
        
        payment.setAssignedAmount(request.getAssignedAmount());
        
        // When amount is assigned, change payment status to PENDING
        if (payment.getPaymentStatus() == StudentPaymentStatus.NOT_APPLICABLE) {
            payment.setPaymentStatus(StudentPaymentStatus.PENDING);
        }
        
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            payment.setNotes(request.getNotes());
        }
        
        return studentPaymentRepository.save(payment);
    }
    
    @Transactional
    public StudentPayment updateStudentPaymentStatus(StudentPaymentStatusUpdateRequest request) {
        try {
            StudentPayment payment = studentPaymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Student payment not found with id: " + request.getPaymentId()));
            
            // Check if status is actually changing to avoid unnecessary updates
            if (payment.getPaymentStatus() != request.getPaymentStatus()) {
                payment.setPaymentStatus(request.getPaymentStatus());
            }
            
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                payment.setNotes(request.getNotes());
            }
            
            return studentPaymentRepository.save(payment);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Payment was modified by another user. Please refresh and try again.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        }
    }
    
    public List<StudentWithStudentPaymentDto> getStudentsWithPaymentByReferralAndCompany() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        List<StudentWithStudentPaymentDto> students;
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Return all students with payment by referral and company
            students = studentRepository.findStudentsWithPaymentByReferralAndCompany();
        } else if ("MANAGER".equalsIgnoreCase(userRole)) {
            // Manager: Return students from their branch with payment by referral and company
            Long branchId = currentUserDetails.getBranchId();
            if (branchId == null) {
                throw new RuntimeException("Manager must be assigned to a branch");
            }
            students = studentRepository.findStudentsWithPaymentByBranch(branchId);
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Referral: Return only students they added
            students = studentRepository.findStudentsWithPaymentByReferral(currentUserDetails.getUserId());
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Company: Return only students they added
            students = studentRepository.findStudentsWithPaymentByCompany(currentUserDetails.getUserId());
        } else {
            // Other roles: Return empty list or throw exception
            throw new RuntimeException("Access denied. This API is only available for ADMIN, MANAGER, REFERRAL, and COMPANY roles.");
        }
        
        // Add dispute status information to each student
        return students.stream()
            .map(this::addDisputeStatusToStudentDto)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Helper method to add dispute status to student DTO
     */
    private StudentWithStudentPaymentDto addDisputeStatusToStudentDto(StudentWithStudentPaymentDto studentDto) {
        if (studentDto.getStudentId() == null) {
            return studentDto;
        }
        
        // Find the most recent active dispute for this student
        List<Dispute> disputes = disputeRepository.findActiveByStudentIdOrderByRaisedAtDesc(studentDto.getStudentId());
        
        if (disputes.isEmpty()) {
            // No disputes found
            studentDto.setDisputeStatus(null);
        } else {
            // Get the status of the most recent dispute
            DisputeStatus disputeStatus = disputes.get(0).getStatus();
            studentDto.setDisputeStatus(disputeStatus);
        }
        
        return studentDto;
    }
    
    @Transactional(readOnly = true)
    public StudentPaymentAmountDto getStudentPaymentAmount(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        StudentPayment payment = studentPaymentRepository.findActiveByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student payment not found for student id: " + studentId));
        
        // Calculate remaining amount
        BigDecimal remainingAmount = BigDecimal.ZERO;
        if (payment.getAssignedAmount() != null) {
            BigDecimal totalPaid = payment.calculateTotalPaid();
            remainingAmount = payment.getAssignedAmount().subtract(totalPaid);
        }
        
        // Create student name
        String studentName = student.getUser() != null ? 
            student.getUser().getFirstName() + " " + student.getUser().getLastName() : 
            "Unknown";
        
        return new StudentPaymentAmountDto(
            payment.getId(),
            student.getId(),
            studentName,
            student.getEmail(),
            payment.getAssignedAmount(),
            payment.calculateTotalPaid(),
            remainingAmount,
            payment.getPaymentStatus(),
            payment.getSourceType(),
            payment.getSourceId(),
            payment.getIsAmountLocked(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
