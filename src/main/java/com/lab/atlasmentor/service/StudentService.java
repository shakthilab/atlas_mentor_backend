package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.dto.*;
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
import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

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
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private FinalPaymentService finalPaymentService;

    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Autowired
    private PaymentDisputeActivityRepository paymentDisputeActivityRepository;
    
    @Autowired
    private ClientPayoutRepository clientPayoutRepository;

    @Autowired
    private ClientPayoutActivityRepository clientPayoutActivityRepository;

    @Autowired
    private ClientPayoutService clientPayoutService;

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
        student.setStatus(StudentStatus.LEAD);
        
        student.setCreatedBy(user.getId());
        
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

    public StudentResponse getStudentByIdAsResponse(Long id) {
        Student student = getStudentById(id);

        StudentResponse response = StudentResponse.fromEntity(student);

        // Populate created by name
        if (student.getCreatedBy() != null) {
            userRepository.findById(student.getCreatedBy()).ifPresent(createdByUser -> {
                response.setCreatedByName(createdByUser.getFullName());
            });
        }

        // Document metadata only (id, name, type) - keeps this load cheap; the actual file
        // bytes are fetched separately, on demand, via downloadDocument(documentId).
        List<DocumentResponse> documents = documentRepository.findByStudentId(id).stream()
                .map(DocumentResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        response.setDocuments(documents);

        // Academic history (10th/12th/Bachelor's/etc.)
        List<AcademicHistoryResponse> academicHistory = studentAcademicHistoryRepository.findByStudentId(id).stream()
                .map(AcademicHistoryResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        response.setAcademicHistory(academicHistory);

        return response;
    }

    private static final Map<String, String> DOCUMENT_MIME_TO_EXTENSION = Map.ofEntries(
            Map.entry("application/pdf", ".pdf"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/jpg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/gif", ".gif"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx")
    );

    /**
     * Saves an onboarding request's documentType -> content map as Document rows. The map
     * carries no original filename, only a base64 string that's often a data URI
     * ("data:<mime>;base64,<data>") - when it is, the mime type is pulled out and stored as
     * fileType, and used to give documentName a real extension (e.g. "10th_marksheet.pdf")
     * instead of a bare key. Without an extension the frontend has no way to know what kind of
     * file it just downloaded, so it saves/opens as an unrecognized file.
     */
    private void saveDocuments(Student student, Map<String, String> documents, Long userId) {
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String documentTypeKey = entry.getKey();
            String content = entry.getValue();
            String mimeType = extractDataUriMimeType(content);
            String extension = mimeType != null ? DOCUMENT_MIME_TO_EXTENSION.get(mimeType.toLowerCase()) : null;
            String fileName = documentTypeKey + (extension != null ? extension : "");

            Document document = new Document();
            document.setStudent(student);
            document.setDocumentType(documentTypeKey);
            document.setDocumentName(fileName);
            document.setFileType(mimeType);
            document.setBase64Content(content);
            document.setCreatedBy(userId);
            documentRepository.save(document);
        }
    }

    private String extractDataUriMimeType(String content) {
        if (content == null || !content.startsWith("data:")) {
            return null;
        }
        int colonIndex = content.indexOf(':');
        int semicolonIndex = content.indexOf(';');
        if (semicolonIndex == -1 || semicolonIndex <= colonIndex) {
            return null;
        }
        return content.substring(colonIndex + 1, semicolonIndex);
    }

    /**
     * Fetches a single document's file bytes for download, decoding the stored base64
     * content on demand. Kept out of {@link #getStudentByIdAsResponse(Long)} so loading a
     * student never pulls every document's bytes - only the one the user clicks download on.
     */
    public DocumentDownload downloadDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found with id: " + documentId));

        if (document.getBase64Content() == null || document.getBase64Content().isBlank()) {
            throw new BusinessException("Document has no file content: " + documentId);
        }

        byte[] bytes = decodeDocumentContent(document.getBase64Content());
        if (bytes == null) {
            throw new BusinessException("Document content is not valid base64: " + documentId);
        }

        String mimeType = resolveMimeType(document, bytes);
        String fileName = resolveFileName(document, mimeType);

        return new DocumentDownload(fileName, mimeType, bytes);
    }

    /**
     * Fetches every document for a student with its content base64-encoded - the list form of
     * {@link #downloadDocument(Long)}, used by GET /api/students/{studentId}/documents so the
     * frontend can render/download all of a student's documents in one call. Unlike
     * downloadDocument, a document with missing or corrupt stored content doesn't fail the
     * whole request: it comes back with `content: null` instead of an error.
     */
    public List<StudentDocumentResponse> getStudentDocuments(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException("Student not found with id: " + studentId);
        }
        return documentRepository.findByStudentId(studentId).stream()
                .map(this::toStudentDocumentResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private StudentDocumentResponse toStudentDocumentResponse(Document document) {
        byte[] bytes = decodeDocumentContent(document.getBase64Content());
        String mimeType = resolveMimeType(document, bytes);
        String fileName = resolveFileName(document, mimeType);
        String content = bytes != null ? java.util.Base64.getEncoder().encodeToString(bytes) : null;
        return new StudentDocumentResponse(document.getId(), document.getDocumentType(), fileName, mimeType, content);
    }

    /**
     * Decodes a document's stored base64 (raw or data-URI form) into bytes, or null if the
     * content is missing/blank/not valid base64 - callers decide whether that's fatal.
     */
    private byte[] decodeDocumentContent(String base64Content) {
        if (base64Content == null || base64Content.isBlank()) {
            return null;
        }
        // Uploads may be stored as a raw base64 string or a data URI
        // (e.g. "data:application/pdf;base64,JVBERi0..."); strip the prefix if present.
        String rawBase64 = base64Content;
        int commaIndex = base64Content.indexOf(',');
        if (base64Content.startsWith("data:") && commaIndex != -1) {
            rawBase64 = base64Content.substring(commaIndex + 1);
        }
        try {
            return java.util.Base64.getDecoder().decode(rawBase64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Older documents (saved before fileType/documentName were captured on upload) have
    // neither column populated - fall back to the data URI prefix, then to sniffing the
    // decoded bytes' signature, so downloads still come back with a real content type and a
    // filename extension the OS/frontend can recognize instead of e.g. "10th_marksheet" with
    // no extension at all.
    private String resolveMimeType(Document document, byte[] bytes) {
        String mimeType = document.getFileType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = extractDataUriMimeType(document.getBase64Content());
        }
        if ((mimeType == null || mimeType.isBlank()) && bytes != null) {
            mimeType = sniffMimeType(bytes);
        }
        return mimeType;
    }

    private String resolveFileName(Document document, String mimeType) {
        String fileName = document.getDocumentName() != null && !document.getDocumentName().isBlank()
                ? document.getDocumentName()
                : (document.getDocumentType() != null ? document.getDocumentType() : ("document-" + document.getId()));
        if (!hasFileExtension(fileName) && mimeType != null) {
            String extension = DOCUMENT_MIME_TO_EXTENSION.get(mimeType.toLowerCase());
            if (extension != null) {
                fileName = fileName + extension;
            }
        }
        return fileName;
    }

    private boolean hasFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 && dotIndex < fileName.length() - 1;
    }

    /** Identifies a file type from its magic bytes when no fileType/data-URI mime is stored. */
    private String sniffMimeType(byte[] bytes) {
        if (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return "application/pdf"; // "%PDF"
        }
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "image/gif";
        }
        return null;
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);

        // Delete payment_transactions before student_payments (FK: payment_transactions.payment_id → student_payments.id)
        paymentTransactionRepository.deleteByStudentId(student.getId());
        studentPaymentRepository.deleteByStudentId(student.getId());

        // Delete legacy PaymentAudit records (foreign key constraint).
        // NOTE: FinancialAuditLog records are intentionally NOT deleted here — they have
        // no FK to Student and are tamper-evident; they must be retained permanently.
        paymentAuditRepository.deleteByStudent(student);

        // Delete related student activities (foreign key constraint)
        studentActivityRepository.deleteByStudent(student);

        // Delete client_payout_activities before client_payouts (FK: client_payout_activities.client_payout_id → client_payouts.id)
        clientPayoutActivityRepository.deleteByStudentId(student.getId());
        clientPayoutRepository.deleteByStudentId(student.getId());

        // The cascade operations will automatically delete:
        // - StudentAcademicHistory records (cascade = CascadeType.ALL)
        // - Document records (cascade = CascadeType.ALL)
        // - Any other related entities with cascade operations

        studentRepository.delete(student);
    }

    public PageResponse<StudentResponse> getAllStudents(StudentStatus status, String search, Pageable pageable) {
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
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) || "ADMINISTRATIVE_ASSISTANT".equalsIgnoreCase(userRole)) {
            // Manager/Branch Partner: Show only branch-specific students
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
        
        // Convert Student entities to StudentResponse DTOs
        List<StudentResponse> studentResponses = students.getContent().stream()
            .map(student -> {
                StudentResponse response = StudentResponse.fromEntity(student);
                // Populate created by name
                if (student.getCreatedBy() != null) {
                    userRepository.findById(student.getCreatedBy()).ifPresent(createdByUser -> {
                        response.setCreatedByName(createdByUser.getFullName());
                    });
                }
                return response;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return PageResponse.of(studentResponses, students.getNumber(), students.getSize(), students.getTotalElements());
    }

    public PageResponse<StudentNonRegisteredResponse> getNonRegisteredStudents(String search, String status, String countryName, String dateFrom, String dateTo, String source, Pageable pageable) {
        var currentUser = SecurityUtils.getCurrentUser();
        
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase() + "%" : "%";
        
        // Build source filter lists
        List<String> sourceRoles = new java.util.ArrayList<>();
        List<String> sourceTypes = new java.util.ArrayList<>();
        
        if (source != null && !source.trim().isEmpty()) {
            String[] sources = source.split(",");
            for (String s : sources) {
                String trimmed = s.trim().toUpperCase();
                switch (trimmed) {
                    case "ADMIN":
                        sourceRoles.add("ADMIN");
                        break;
                    case "COUNSELLOR":
                    case "COUNSELOR":
                        sourceRoles.add("JUNIOR_COUNSELLOR");
                        sourceRoles.add("SENIOR_COUNSELLOR");
                        break;
                    case "REFERRAL":
                        sourceRoles.add("REFERRAL");
                        sourceTypes.add("REFERRAL");
                        break;
                    case "COMPANY":
                        sourceRoles.add("COMPANY");
                        sourceTypes.add("COMPANY");
                        break;
                    case "MANAGER":
                        sourceRoles.add("MANAGER");
                        break;
                    case "BRANCH_PARTNER":
                        sourceRoles.add("BRANCH_PARTNER");
                        break;
                    case "ADMINISTRATIVE_ASSISTANT":
                        sourceRoles.add("ADMINISTRATIVE_ASSISTANT");
                        break;
                }
            }
        }
        
        // Use empty lists if no valid sources provided (will be ignored by IS NULL check)
        if (sourceRoles.isEmpty()) {
            sourceRoles = null;
        }
        if (sourceTypes.isEmpty()) {
            sourceTypes = null;
        }
        
        Page<Student> students;
        
        // Check user role and apply appropriate filtering
        String userRole = currentUser.getRole();
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Show all branch students except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                true, 
                null, 
                status,
                countryName,
                dateFrom,
                dateTo,
                source,
                sourceRoles,
                sourceTypes,
                pageable
            );
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) || "ADMINISTRATIVE_ASSISTANT".equalsIgnoreCase(userRole)) {
            // Manager/Branch Partner: Show only branch-specific students except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                false, 
                currentUser.getBranchId(), 
                status,
                countryName,
                dateFrom,
                dateTo,
                source,
                sourceRoles,
                sourceTypes,
                pageable
            );
        } else if ("JUNIOR_COUNSELLOR".equalsIgnoreCase(userRole) || "SENIOR_COUNSELLOR".equalsIgnoreCase(userRole)) {
            // Counsellors: Show only assigned students to them except REGISTERED
            students = studentRepository.findByNonRegisteredStatusForCounsellor(
                searchParam, 
                currentUser.getUserId(),
                status,
                countryName,
                dateFrom,
                dateTo,
                source,
                sourceRoles,
                sourceTypes,
                pageable
            );
        } else if ("REFERRAL".equalsIgnoreCase(userRole) || "COMPANY".equalsIgnoreCase(userRole)) {
            // Referral and Company: Show only students they created except REGISTERED
            students = studentRepository.findByNonRegisteredStatusForCreator(
                searchParam, 
                currentUser.getUserId(),
                status,
                countryName,
                dateFrom,
                dateTo,
                source,
                sourceRoles,
                sourceTypes,
                pageable
            );
        } else {
            // Default: Apply branch-based access control (for other roles) except REGISTERED
            students = studentRepository.findByNonRegisteredStatusWithAccess(
                searchParam, 
                currentUser.isAdmin(), 
                currentUser.getBranchId(),
                status,
                countryName,
                dateFrom,
                dateTo,
                source,
                sourceRoles,
                sourceTypes,
                pageable
            );
        }
        
        // Convert Student entities to StudentNonRegisteredResponse DTOs
        List<StudentNonRegisteredResponse> studentResponses = students.getContent().stream().map(student -> {
            StudentNonRegisteredResponse response = new StudentNonRegisteredResponse();
            
            // Basic student information
            response.setId(student.getId());
            // User information (nested user object)
            if (student.getUser() != null) {
                StudentNonRegisteredResponse.UserDto userDto = new StudentNonRegisteredResponse.UserDto();
                userDto.setId(student.getUser().getId());
                userDto.setFirstName(student.getUser().getFirstName());
                userDto.setLastName(student.getUser().getLastName());
                userDto.setFullName(student.getUser().getFullName());
                userDto.setEmail(student.getUser().getEmail());
                userDto.setPhone(student.getUser().getPhone());
                userDto.setCreatedAt(student.getUser().getCreatedAt() != null ? student.getUser().getCreatedAt().toString() : null);
                
                // User role information
                if (student.getUser().getRole() != null) {
                    userDto.setRole(student.getUser().getRole().getName());
                }
                
                // User branch information
                if (student.getUser().getBranch() != null) {
                    userDto.setBranchId(student.getUser().getBranch().getId());
                    userDto.setBranchName(student.getUser().getBranch().getName());
                }
                
                response.setUser(userDto);
            }
            response.setNotes(student.getNotes());
            response.setCourseName(student.getCourseName());
            response.setIntakePeriod(student.getIntakePeriod());
            response.setSource(student.getSource());
            response.setStatus(student.getStatus().name());
            response.setCreatedAt(student.getCreatedAt() != null ? student.getCreatedAt().toString() : null);
            response.setCreatedBy(student.getCreatedBy());
            response.setUpdatedBy(student.getUpdatedBy());
            response.setPriority(student.getPriority());
            response.setPriorityDisplayName(student.getPriority() != null ? student.getPriority().getDisplayLabel() : null);
            response.setPrioritySubCategory(student.getPrioritySubCategory());
            response.setPrioritySubCategoryDisplayName(
                    student.getPrioritySubCategory() != null ? student.getPrioritySubCategory().getLabel() : null);
            response.setBackground(student.getBackground());
            response.setBackgroundDisplayName(student.getBackground() != null ? student.getBackground().getDisplayLabel() : null);

            // Mobile country code (get from user since it's now stored in User entity)
            if (student.getUser() != null && student.getUser().getMobileCountryCode() != null) {
                response.setMobileCountryCode(new MobileCountryCodeDto(
                    student.getUser().getMobileCountryCode().getId(),
                    student.getUser().getMobileCountryCode().getCountryName(),
                    student.getUser().getMobileCountryCode().getCountryCode(),
                    student.getUser().getMobileCountryCode().getMobileCode(),
                    student.getUser().getMobileCountryCode().getIsoAlpha2(),
                    student.getUser().getMobileCountryCode().getIsoAlpha3(),
                    student.getUser().getMobileCountryCode().getIsActive(),
                    student.getUser().getMobileCountryCode().getFlagUrl(),
                    student.getUser().getMobileCountryCode().getMobileNumberLength()
                ));
            } else {
                // Always set MCC object, even if null, to ensure consistent API response
                response.setMobileCountryCode(new MobileCountryCodeDto());
            }
            
            // Branch information
            if (student.getBranch() != null) {
                response.setBranchId(student.getBranch().getId());
                response.setBranchName(student.getBranch().getName());
                response.setBranchLocation(student.getBranch().getLocation());
            }
            
            // Country information
            if (student.getCountry() != null) {
                response.setCountryId(student.getCountry().getId());
                response.setCountryName(student.getCountry().getName());
            }
            
            // University information
            if (student.getUniversity() != null) {
                response.setUniversityId(student.getUniversity().getId());
                response.setUniversityName(student.getUniversity().getName());
            }
            
            // Assigned by information (full user object)
            if (student.getAssignedBy() != null) {
                StudentNonRegisteredResponse.UserDto assignedByDto = new StudentNonRegisteredResponse.UserDto();
                assignedByDto.setId(student.getAssignedBy().getId());
                assignedByDto.setFirstName(student.getAssignedBy().getFirstName());
                assignedByDto.setLastName(student.getAssignedBy().getLastName());
                assignedByDto.setFullName(student.getAssignedBy().getFullName());
                assignedByDto.setEmail(student.getAssignedBy().getEmail());
                assignedByDto.setPhone(student.getAssignedBy().getPhone());
                assignedByDto.setCreatedAt(student.getAssignedBy().getCreatedAt().toString());
                
                // Assigned by user role information
                if (student.getAssignedBy().getRole() != null) {
                    assignedByDto.setRole(student.getAssignedBy().getRole().getName());
                }
                
                // Assigned by user branch information
                if (student.getAssignedBy().getBranch() != null) {
                    assignedByDto.setBranchId(student.getAssignedBy().getBranch().getId());
                    assignedByDto.setBranchName(student.getAssignedBy().getBranch().getName());
                }
                
                response.setAssignedBy(assignedByDto);
            }
            
            // Created by information (full user object)
            if (student.getCreatedBy() != null) {
                userRepository.findById(student.getCreatedBy()).ifPresent(createdByUser -> {
                    StudentNonRegisteredResponse.UserDto createdByDto = new StudentNonRegisteredResponse.UserDto();
                    createdByDto.setId(createdByUser.getId());
                    createdByDto.setFirstName(createdByUser.getFirstName());
                    createdByDto.setLastName(createdByUser.getLastName());
                    createdByDto.setFullName(createdByUser.getFullName());
                    createdByDto.setEmail(createdByUser.getEmail());
                    createdByDto.setPhone(createdByUser.getPhone());
                    createdByDto.setCreatedAt(createdByUser.getCreatedAt() != null ? createdByUser.getCreatedAt().toString() : null);
                    
                    // Created by user role information
                    if (createdByUser.getRole() != null) {
                        createdByDto.setRole(createdByUser.getRole().getName());
                    }
                    
                    // Created by user branch information
                    if (createdByUser.getBranch() != null) {
                        createdByDto.setBranchId(createdByUser.getBranch().getId());
                        createdByDto.setBranchName(createdByUser.getBranch().getName());
                    }
                    
                    response.setCreatedByUser(createdByDto);
                });
            }
            
            return response;
        }).collect(java.util.stream.Collectors.toList());
        
        return PageResponse.of(studentResponses, students.getNumber(), students.getSize(), students.getTotalElements());
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

    private String normalizeEmail(String email) {
        return (email == null || email.trim().isEmpty()) ? null : email.trim();
    }

    @Transactional
    public Student createOrUpdateStudent(StudentOnboardingRequest request) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        User currentUser = userRepository.findById(currentUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Check if student already exists with this email (only if email is provided)
        Student existingStudent = null;
        
        // Check by email first
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            existingStudent = studentRepository.findByEmail(request.getEmail()).orElse(null);
        }
        
        // If no email match, check by phone number
        if (existingStudent == null && request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            existingStudent = studentRepository.findByPhone(request.getPhone()).orElse(null);
        }
        
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

    /**
     * priority/prioritySubCategory/background are all optional — a lead can be created or
     * edited without being classified. The one rule enforced here: when prioritySubCategory
     * is set, it must belong to the priority tier submitted alongside it (e.g. priority=P1
     * with prioritySubCategory=WARM_LEADS, which is a P2 subcategory, is rejected). The
     * tier/subcategory mapping itself lives in LeadPrioritySubCategory — this is the one call
     * site for the manual create/edit path; LeadImportService applies the same check
     * (LeadPrioritySubCategory.requireBelongsToTier) to each import row.
     */
    private void validateLeadClassification(StudentOnboardingRequest request) {
        try {
            LeadPrioritySubCategory.requireBelongsToTier(request.getPrioritySubCategory(), request.getPriority());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private Student createNewStudent(StudentOnboardingRequest request, User currentUser, CustomUserDetails currentUserDetails) {
        // No email validation for student creation - users with rights can create students without email
        validateLeadClassification(request);

        // Generate random password
        String generatedPassword = generateRandomPassword();
        
        try {
            // Create user account (even with null email)
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(normalizeEmail(request.getEmail())); // Can be null
            user.setPhone(request.getPhone());
            user.setPassword(passwordEncoder.encode(generatedPassword));
            user.setCreatedBy(currentUser.getId());
            
            // Set mobile country code if provided
            if (request.getMobileCountryCodeId() != null) {
                MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(request.getMobileCountryCodeId())
                    .orElseThrow(() -> new RuntimeException("Mobile country code not found"));
                user.setMobileCountryCode(mobileCountryCode);
            }
            
            // Set role - fetch STUDENT role from cache
            Role studentRole = roleCacheService.getRoleByName("STUDENT");
            user.setRole(studentRole);
            
            user = userRepository.save(user);
        
        // Create student record
        Student student = new Student();
        student.setUser(user);
        student.setEmail(normalizeEmail(request.getEmail()));
        student.setStatus(StudentStatus.LEAD);
        student.setCreatedBy(currentUser.getId());
        
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
        student.setSource(request.getSource());
        student.setPriority(request.getPriority());
        student.setPrioritySubCategory(request.getPrioritySubCategory());
        student.setBackground(request.getBackground());

        // Save student first
        student = studentRepository.save(student);
        
        // Handle assignedToId if provided
        if (request.getAssignedToId() != null) {
            User assignedToUser = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + request.getAssignedToId()));
            student.setAssignedBy(assignedToUser);
            student = studentRepository.save(student); // Save again to update the assignedBy relationship
        }
        
        // Save academic history
        saveAcademicHistory(student, request.getAcademicHistory(), currentUser.getId());

        // Save documents
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            saveDocuments(student, request.getDocuments(), currentUser.getId());
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
        
        // Send login credentials if an email was provided at creation
        String createdStudentEmail = normalizeEmail(request.getEmail());
        if (createdStudentEmail != null) {
            try {
                emailService.sendLoginCredentials(createdStudentEmail, generatedPassword);
            } catch (Exception e) {
                // Log error but don't fail the student creation
                System.err.println("Failed to send login credentials email: " + e.getMessage());
            }
        }

        return student;
        
        } catch (DataIntegrityViolationException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("email")) {
                    throw new BusinessException("Email already exists");
                }
                if (errorMessage.contains("phone") || errorMessage.contains("uk_users_phone")) {
                    throw new BusinessException("Phone number already exists");
                }
            }
            throw new RuntimeException("Database constraint violation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create student: " + e.getMessage());
        }
    }

    private Student updateStudentData(Student student, StudentOnboardingRequest request, User currentUser) {
        validateLeadClassification(request);
        String newEmail = normalizeEmail(request.getEmail());
        String credentialsEmail = null;
        String credentialsPassword = null;

        // Update user information if user exists
        if (student.getUser() != null) {
            User user = student.getUser();

            // Check if phone number is being changed and if new phone already exists (exclude current user)
            if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
                if (userRepository.existsByPhoneExcludingUser(request.getPhone(), user.getId())) {
                    throw new BusinessException("Phone number already exists");
                }
            }

            // Email being added or changed - generate fresh login credentials to send
            boolean emailAddedOrChanged = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());

            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(newEmail);
            user.setPhone(request.getPhone());

            // Update mobile country code if provided
            if (request.getMobileCountryCodeId() != null) {
                MobileCountryCode mobileCountryCode = mobileCountryCodeRepository.findById(request.getMobileCountryCodeId())
                    .orElseThrow(() -> new RuntimeException("Mobile country code not found"));
                user.setMobileCountryCode(mobileCountryCode);
            }

            if (emailAddedOrChanged) {
                credentialsPassword = generateRandomPassword();
                user.setPassword(passwordEncoder.encode(credentialsPassword));
                credentialsEmail = newEmail;
            }

            userRepository.save(user);
        }

        // Update student information
        student.setEmail(newEmail);
        
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
        student.setSource(request.getSource());
        student.setPriority(request.getPriority());
        student.setPrioritySubCategory(request.getPrioritySubCategory());
        student.setBackground(request.getBackground());
        student.setUpdatedBy(currentUser.getId());
        
        // Handle assignedToId if provided
        if (request.getAssignedToId() != null) {
            User assignedToUser = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + request.getAssignedToId()));
            student.setAssignedBy(assignedToUser);
        }
        
        // Update academic history - only touch it when the caller actually sends
        // academicHistory; omitting it from an update payload (e.g. a status-only or
        // documents-only edit) must not wipe out history saved by an earlier request.
        if (request.getAcademicHistory() != null) {
            studentAcademicHistoryRepository.deleteByStudentId(student.getId());
            saveAcademicHistory(student, request.getAcademicHistory(), currentUser.getId());
        }

        // Update documents - same rule: only replace them when the request explicitly
        // carries a documents map. Previously this deleted unconditionally on every update,
        // so any edit that didn't resend documents silently deleted them for good.
        if (request.getDocuments() != null) {
            documentRepository.deleteByStudentId(student.getId());
            saveDocuments(student, request.getDocuments(), currentUser.getId());
        }
        
        Student savedStudent = studentRepository.save(student);

        // Send fresh login credentials now that the email change is persisted
        if (credentialsEmail != null) {
            try {
                emailService.sendLoginCredentials(credentialsEmail, credentialsPassword);
            } catch (Exception e) {
                // Log error but don't fail the update
                System.err.println("Failed to send login credentials email: " + e.getMessage());
            }
        }

        return savedStudent;
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
    
    // Phone validation is now handled at User entity level with database constraints

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

    private void saveAcademicHistory(Student student,
                                     List<StudentOnboardingRequest.AcademicEntry> entries,
                                     Long createdBy) {
        if (entries == null) return;
        for (StudentOnboardingRequest.AcademicEntry entry : entries) {
            if (entry == null) continue;
            saveAcademicEntry(student, entry, createdBy);
        }
    }

    private void saveAcademicEntry(Student student,
                                   StudentOnboardingRequest.AcademicEntry entry, Long createdBy) {
        StudentAcademicHistory record = new StudentAcademicHistory();
        record.setStudent(student);
        record.setQualification(entry.getQualification());
        record.setInstitutionName(entry.getInstitutionName());
        record.setBoardUniversity(entry.getBoardUniversity());
        record.setPassingYear(entry.getPassingYear());
        record.setScore(entry.getScore());
        record.setStream(entry.getStream());
        record.setCreatedBy(createdBy);
        studentAcademicHistoryRepository.save(record);
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
            // Reason is required when marking as LOST
            if (newStatus == StudentStatus.LOST) {
                if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                    throw new BusinessException("Reason is required when marking student as LOST");
                }
                student.setLostReason(request.getReason());
            }

            // Check if email is mandatory for REGISTERED status
            if (newStatus == StudentStatus.REGISTERED) {
                if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
                    throw new BusinessException("Email is mandatory to convert student to registered");
                }
                
                // Handle user account creation and email sending for REGISTERED status
                try {
                    if (student.getUser() == null) {
                        // Create new user account
                        String generatedPassword = generateRandomPassword();
                        
                        User user = new User();
                        // Use email prefix as default first name
                        String emailPrefix = student.getEmail().split("@")[0];
                        user.setFirstName(emailPrefix);
                        user.setLastName("");
                        user.setEmail(student.getEmail());
                        user.setPhone(student.getUser() != null ? student.getUser().getPhone() : null);
                        user.setPassword(passwordEncoder.encode(generatedPassword));
                        user.setCreatedBy(currentUser.getId());
                        
                        // Set role - fetch STUDENT role from cache
                        Role studentRole = roleCacheService.getRoleByName("STUDENT");
                        user.setRole(studentRole);
                        
                        user = userRepository.save(user);
                        student.setUser(user);
                        
                        // Send login credentials via email
                        emailService.sendLoginCredentials(student.getEmail(), generatedPassword);
                    } else {
                        // User already exists - generate new password and update user account
                        String generatedPassword = generateRandomPassword();
                        
                        // Update the existing user's password
                        User existingUser = student.getUser();
                        existingUser.setPassword(passwordEncoder.encode(generatedPassword));
                        existingUser.setUpdatedBy(currentUser.getId());
                        userRepository.save(existingUser);
                        
                        // Send login credentials with the new password
                        emailService.sendLoginCredentials(student.getEmail(), generatedPassword);
                    }
                } catch (Exception e) {
                    // Log error but don't fail the status change
                    System.err.println("Failed to handle user account or send email: " + e.getMessage());
                }
            }
            
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
    
    @Transactional
    public String updateStudentActiveStatus(Long studentId, String status) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new BusinessException("Student not found with id: " + studentId));

        if (student.getUser() == null) {
            throw new BusinessException("Student does not have an associated user account");
        }

        com.lab.atlasmentor.enums.UserStatus newStatus = com.lab.atlasmentor.enums.UserStatus.valueOf(status.toUpperCase());
        com.lab.atlasmentor.enums.UserStatus currentStatus = student.getUser().getStatus();

        userRepository.updateUserStatus(student.getUser().getId(), newStatus);

        if (currentStatus == newStatus) {
            return "already_" + status.toLowerCase();
        }
        return status.toLowerCase();
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
            throw new BusinessException("Payment amount is locked and cannot be modified");
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
    
    public ClientPayout updateClientPayoutAmount(ClientPayoutAmountUpdateRequest request) {
        ClientPayout payout = clientPayoutRepository.findById(request.getClientPayoutId())
            .orElseThrow(() -> new RuntimeException("Client payout not found with id: " + request.getClientPayoutId()));
        
        // Validate that the payout can be updated
        if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.ACCEPTED) {
            throw new BusinessException("Cannot update amount for an accepted payout");
        }
        
        if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.DISPUTE) {
            throw new BusinessException("Cannot update amount for a disputed payout");
        }
        
        BigDecimal oldAmount = payout.getAssignedAmount();
        payout.setAssignedAmount(request.getAssignedAmount());
        
        // Update status based on new amount
        payout.updateStatusBasedOnPayment();
        
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            payout.setNotes(request.getNotes());
        }
        
        // Set assignment details
        var currentUserDetails = SecurityUtils.getCurrentUser();
        payout.setAssignedBy(userRepository.findById(currentUserDetails.getUserId()).orElse(null));
        payout.setAssignedAt(java.time.LocalDateTime.now());
        
        ClientPayout savedPayout = clientPayoutRepository.save(payout);
        
        // Log activity
        clientPayoutService.logActivity(
            savedPayout, 
            com.lab.atlasmentor.enums.ClientPayoutAction.AMOUNT_ASSIGNED,
            oldAmount != null ? oldAmount.toString() : "0", 
            request.getAssignedAmount().toString(), 
            request.getNotes(), 
            currentUserDetails.getUserId()
        );
        
        return savedPayout;
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
            throw new BusinessException("Payment was modified by another user. Please refresh and try again.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        }
    }
    
    public List<ClientPayoutDto> getStudentsWithPaymentByReferralAndCompany() {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        List<ClientPayout> clientPayouts;
        
        List<SourceType> sourceTypes = List.of(SourceType.REFERRAL, SourceType.COMPANY);
        
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Return all client payouts with referral and company source types
            clientPayouts = clientPayoutRepository.findBySourceTypeIn(sourceTypes);
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) || "ADMINISTRATIVE_ASSISTANT".equalsIgnoreCase(userRole)) {
            // Manager/Branch Partner: Return client payouts from their branch with referral and company source types
            Long branchId = currentUserDetails.getBranchId();
            if (branchId == null) {
                throw new BusinessException("Manager must be assigned to a branch");
            }
            clientPayouts = clientPayoutRepository.findByBranchIdAndSourceTypeIn(branchId, sourceTypes);
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Referral: Return only their client payouts
            clientPayouts = clientPayoutRepository.findByUserIdAndSourceType(currentUserDetails.getUserId(), SourceType.REFERRAL);
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Company: Return only their client payouts
            clientPayouts = clientPayoutRepository.findByUserIdAndSourceType(currentUserDetails.getUserId(), SourceType.COMPANY);
        } else {
            // Other roles: Return empty list or throw exception
            throw new BusinessException("Access denied. This API is only available for ADMIN, MANAGER, REFERRAL, and COMPANY roles.");
        }
        
        // Convert to DTOs
        return clientPayouts.stream()
                .map(this::convertToClientPayoutDto)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public ClientPayoutWithSummaryDto getStudentsWithPaymentByReferralAndCompanyWithSummary(
            String search, String source, Long branch, String paymentStatus, String dateFrom, String dateTo, int page, int size) {
        var currentUserDetails = SecurityUtils.getCurrentUser();
        String userRole = currentUserDetails.getRole();
        List<ClientPayout> clientPayouts;

        // Parse filter parameters
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        SourceType sourceParam = null;
        if (source != null && !source.trim().isEmpty()) {
            try {
                sourceParam = SourceType.valueOf(source.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid source type. Valid values: REFERRAL, COMPANY");
            }
        }

        ClientPayoutStatus paymentStatusParam = null;
        if (paymentStatus != null && !paymentStatus.trim().isEmpty()) {
            try {
                paymentStatusParam = ClientPayoutStatus.valueOf(paymentStatus.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid payment status. Valid values: " + java.util.Arrays.toString(ClientPayoutStatus.values()));
            }
        }

        LocalDateTime dateFromParam = null;
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            try {
                dateFromParam = LocalDateTime.parse(dateFrom.trim());
            } catch (Exception e) {
                throw new BusinessException("Invalid dateFrom format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
            }
        }

        LocalDateTime dateToParam = null;
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            try {
                dateToParam = LocalDateTime.parse(dateTo.trim());
            } catch (Exception e) {
                throw new BusinessException("Invalid dateTo format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
            }
        }

        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admin: Use advanced filtering
            clientPayouts = clientPayoutRepository.findWithFiltersForAdmin(
                searchParam, sourceParam != null ? sourceParam.name() : null, branch,
                paymentStatusParam != null ? paymentStatusParam.name() : null, dateFromParam, dateToParam);
        } else if ("MANAGER".equalsIgnoreCase(userRole) || "BRANCH_PARTNER".equalsIgnoreCase(userRole) || "ADMINISTRATIVE_ASSISTANT".equalsIgnoreCase(userRole)) {
            // Manager/Branch Partner: Use branch-specific filtering
            Long branchId = currentUserDetails.getBranchId();
            if (branchId == null) {
                throw new BusinessException("Manager must be assigned to a branch");
            }
            // Override branch parameter with user's branch for security
            clientPayouts = clientPayoutRepository.findWithFiltersForBranch(
                branchId, searchParam, sourceParam != null ? sourceParam.name() : null,
                paymentStatusParam != null ? paymentStatusParam.name() : null, dateFromParam, dateToParam);
        } else if ("REFERRAL".equalsIgnoreCase(userRole)) {
            // Referral: Use user-specific filtering
            clientPayouts = clientPayoutRepository.findWithFiltersForUser(
                currentUserDetails.getUserId(), SourceType.REFERRAL.name(), searchParam,
                paymentStatusParam != null ? paymentStatusParam.name() : null, dateFromParam, dateToParam);
        } else if ("COMPANY".equalsIgnoreCase(userRole)) {
            // Company: Use user-specific filtering
            clientPayouts = clientPayoutRepository.findWithFiltersForUser(
                currentUserDetails.getUserId(), SourceType.COMPANY.name(), searchParam,
                paymentStatusParam != null ? paymentStatusParam.name() : null, dateFromParam, dateToParam);
        } else {
            // Other roles: Return empty list or throw exception
            throw new BusinessException("Access denied. This API is only available for ADMIN, MANAGER, REFERRAL, and COMPANY roles.");
        }

        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, clientPayouts.size());
        List<ClientPayout> paginatedPayouts = clientPayouts.subList(startIndex, endIndex);

        // Convert to DTOs
        List<ClientPayoutDto> payoutDtos = paginatedPayouts.stream()
                .map(this::convertToClientPayoutDto)
                .collect(java.util.stream.Collectors.toList());

        // Calculate summary statistics based on filtered results
        ClientPayoutSummaryDto summary = calculateClientPayoutSummary(clientPayouts);

        return new ClientPayoutWithSummaryDto(payoutDtos, summary);
    }

    private ClientPayoutSummaryDto calculateClientPayoutSummary(List<ClientPayout> clientPayouts) {
        // Initialize counters
        long totalAssigned = 0;
        long totalPaid = 0;
        long totalPending = 0;
        long pendingApprovals = 0;
        long disputes = 0;
        long rejected = 0;
        long partialPayments = 0;

        // Initialize amount totals
        java.math.BigDecimal totalAssignedAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPaidAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPendingAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalDisputedAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalRejectedAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPartialAmount = java.math.BigDecimal.ZERO;

        for (ClientPayout payout : clientPayouts) {
            // Count-based statistics
            if (payout.getAssignedAmount() != null && payout.getAssignedAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totalAssigned++;
                totalAssignedAmount = totalAssignedAmount.add(payout.getAssignedAmount());

                if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.PAID) {
                    totalPaid++;
                    totalPaidAmount = totalPaidAmount.add(payout.getPaidAmount() != null ? payout.getPaidAmount() : java.math.BigDecimal.ZERO);
                } else if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.PARTIAL_PAID) {
                    partialPayments++;
                    totalPartialAmount = totalPartialAmount.add(payout.getPaidAmount() != null ? payout.getPaidAmount() : java.math.BigDecimal.ZERO);
                    totalPendingAmount = totalPendingAmount.add(
                        payout.getAssignedAmount().subtract(payout.getPaidAmount() != null ? payout.getPaidAmount() : java.math.BigDecimal.ZERO)
                    );
                } else if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.AMOUNT_ASSIGNED) {
                    pendingApprovals++;
                    totalPendingAmount = totalPendingAmount.add(payout.getAssignedAmount());
                }
            } else {
                totalPending++;
            }

            if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.DISPUTE) {
                disputes++;
                totalDisputedAmount = totalDisputedAmount.add(
                    payout.getDisputeAmount() != null ? payout.getDisputeAmount() :
                    (payout.getAssignedAmount() != null ? payout.getAssignedAmount() : java.math.BigDecimal.ZERO)
                );
            }

            if (payout.getPayoutStatus() == com.lab.atlasmentor.enums.ClientPayoutStatus.REJECTED) {
                rejected++;
                totalRejectedAmount = totalRejectedAmount.add(
                    payout.getAssignedAmount() != null ? payout.getAssignedAmount() : java.math.BigDecimal.ZERO
                );
            }
        }

        ClientPayoutSummaryDto summary = new ClientPayoutSummaryDto(
            totalAssigned, totalPaid, totalPending, pendingApprovals, disputes,
            totalAssignedAmount, totalPaidAmount, totalPendingAmount, totalDisputedAmount,
            partialPayments, totalPartialAmount
        );

        // Set rejected values after construction
        summary.setRejected(rejected);
        summary.setTotalRejectedAmount(totalRejectedAmount);
        return summary;
    }
    
    
    private PaymentDisputeActivityDto convertToDisputeDto(PaymentDisputeActivity activity) {
        UserInfoDto doneByDto = null;
        if (activity.getDoneBy() != null) {
            User user = activity.getDoneBy();
            doneByDto = new UserInfoDto(
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getName() : null
            );
        }

        PaymentDisputeActivityDto dto = new PaymentDisputeActivityDto(
                activity.getId(),
                activity.getPayment() != null ? activity.getPayment().getId() : null,
                activity.getAction(),
                activity.getOldValue(),
                activity.getNewValue(),
                activity.getReason(),
                doneByDto,
                activity.getDoneAt()
        );
        dto.setStatus(activity.getStatus());
        dto.setUpdatedAt(activity.getUpdatedAt());
        return dto;
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
    
    public ClientPayoutDto convertToClientPayoutDto(ClientPayout payout) {
        ClientPayoutDto dto = new ClientPayoutDto();
        dto.setId(payout.getId());
        dto.setStudentId(payout.getStudentId());
        dto.setStudentName(payout.getStudent() != null && payout.getStudent().getUser() != null ? 
            payout.getStudent().getUser().getFirstName() + " " + payout.getStudent().getUser().getLastName() : null);
        dto.setSourceType(payout.getSourceType());
        dto.setAssignedAmount(payout.getAssignedAmount());
        dto.setPaidAmount(payout.getPaidAmount());
        dto.setBalanceAmount(payout.getBalanceAmount());
        dto.setSettledAmount(payout.getSettledAmount());
        
        // Use enum display logic for status and payment stage
        dto.setPayoutStatus(payout.getPayoutStatus().getEffectiveStatus(payout.getAssignedAmount()));
        dto.setPaymentStageDisplay(payout.getPayoutStatus().getDisplayStatus(payout.getAssignedAmount()));
        
        dto.setPreviousStatus(payout.getPreviousStatus());
        dto.setPaymentProgress(payout.getPaymentProgress());
        
        // Dispute tracking
        dto.setDisputeReason(payout.getDisputeReason());
        dto.setDisputeResponse(payout.getDisputeResponse());
        dto.setDisputeAmount(payout.getDisputeAmount());
        dto.setDisputedAt(payout.getDisputedAt());
        dto.setRespondedAt(payout.getRespondedAt());
        
        // User tracking
        if (payout.getUser() != null) {
            dto.setUser(convertToUserInfoDto(payout.getUser()));
        }
        if (payout.getAssignedBy() != null) {
            dto.setAssignedBy(convertToUserInfoDto(payout.getAssignedBy()));
        }
        if (payout.getDisputedBy() != null) {
            dto.setDisputedBy(convertToUserInfoDto(payout.getDisputedBy()));
        }
        if (payout.getRespondedBy() != null) {
            dto.setRespondedBy(convertToUserInfoDto(payout.getRespondedBy()));
        }
        if (payout.getLastPaidBy() != null) {
            dto.setLastPaidBy(convertToUserInfoDto(payout.getLastPaidBy()));
        }
        
        return dto;
    }
    
    private UserInfoDto convertToUserInfoDto(User user) {
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        return dto;
    }
}
