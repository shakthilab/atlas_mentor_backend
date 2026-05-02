package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.PageResponse;
import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.StudentNote;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.Country;
import com.lab.atlasmentor.model.University;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.MobileCountryCode;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.StudentNoteRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.CountryRepository;
import com.lab.atlasmentor.repository.UniversityRepository;
import com.lab.atlasmentor.repository.BranchRepository;
import com.lab.atlasmentor.repository.MobileCountryCodeRepository;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        
        student.setCreatedBy(user);
        
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

    public PageResponse<Student> getAllStudents(StudentStatus status, String search, Pageable pageable) {
        var currentUser = SecurityUtils.getCurrentUser();
        
        if (status != null || (search != null && !search.trim().isEmpty())) {
            // Use existing filter method for complex searches
            String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase() + "%" : "%";
            Page<Student> students = studentRepository.findByFilters(status, searchParam, pageable);
            return PageResponse.of(students.getContent(), students.getNumber(), students.getSize(), students.getTotalElements());
        } else {
            // Use branch-based access control for simple getAll
            Page<Student> students = studentRepository.findAllWithAccess(currentUser.isAdmin(), currentUser.getBranchId(), pageable);
            return PageResponse.of(students.getContent(), students.getNumber(), students.getSize(), students.getTotalElements());
        }
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
}
