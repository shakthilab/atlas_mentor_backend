package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.StudentRegistrationRequest;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.StudentNote;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.StudentNoteRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.enums.StudentStatus;
import org.springframework.beans.factory.annotation.Autowired;
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
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setStatus(StudentStatus.LEAD);
        student.setBranchId(1L); // Default branch - you may want to make this configurable
        student.setCreatedBy(user);
        
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

    private boolean hasAcademicData(StudentRegistrationRequest request) {
        return (request.getPreferredCountry() != null && !request.getPreferredCountry().trim().isEmpty()) ||
               (request.getPreferredUniversity() != null && !request.getPreferredUniversity().trim().isEmpty()) ||
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
        note.setPreferredCountry(request.getPreferredCountry());
        note.setPreferredUniversity(request.getPreferredUniversity());
        note.setCourse(request.getCourse());
        note.setIntake(request.getIntake());
        note.setReferralCode(request.getReferralCode());
        note.setAcademicDetails(request.getBasicAcademicDetails());
        note.setAdditionalNotes(request.getOptionalNotes());
        
        return note;
    }
}
