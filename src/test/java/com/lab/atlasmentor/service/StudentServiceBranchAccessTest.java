package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.StudentResponse;
import com.lab.atlasmentor.enums.StudentStatus;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceBranchAccessTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private CustomUserDetails adminUser;
    private CustomUserDetails nonAdminUser;
    private Pageable pageable;
    private List<Student> allBranchStudents;
    private List<Student> branch1Students;

    @BeforeEach
    void setUp() {
        // Create test users
        adminUser = new CustomUserDetails(1L, "admin@test.com", "ADMIN", null);
        nonAdminUser = new CustomUserDetails(2L, "user@test.com", "COUNSELLOR", 1L);

        // Create test pageable
        pageable = PageRequest.of(0, 10);

        // Create test students
        Student student1 = new Student();
        student1.setId(1L);
        student1.setEmail("student1@test.com");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setEmail("student2@test.com");

        Student student3 = new Student();
        student3.setId(3L);
        student3.setEmail("student3@test.com");

        allBranchStudents = Arrays.asList(student1, student2, student3);
        branch1Students = Arrays.asList(student1, student2);
    }

    @Test
    void testGetAllStudents_AdminSeesAllBranches() {
        // Given
        Page<Student> expectedPage = new PageImpl<>(allBranchStudents, pageable, allBranchStudents.size());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(adminUser);
            
            when(studentRepository.findByFiltersWithAccess(
                eq(null), 
                eq("%"), 
                eq(true), 
                eq(null), 
                eq(pageable)
            )).thenReturn(expectedPage);

            // When
            var result = studentService.getAllStudents(null, null, pageable);

            // Then
            assertEquals(3, result.getContent().size());
            assertEquals(3, result.getTotalElements());
            
            // Verify the repository was called with admin parameters
            verify(studentRepository).findByFiltersWithAccess(
                eq(null), 
                eq("%"), 
                eq(true), 
                eq(null), 
                eq(pageable)
            );
        }
    }

    @Test
    void testGetAllStudents_NonAdminSeesOnlyTheirBranch() {
        // Given
        Page<Student> expectedPage = new PageImpl<>(branch1Students, pageable, branch1Students.size());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(nonAdminUser);
            
            when(studentRepository.findByFiltersWithAccess(
                eq(null), 
                eq("%"), 
                eq(false), 
                eq(1L), 
                eq(pageable)
            )).thenReturn(expectedPage);

            // When
            var result = studentService.getAllStudents(null, null, pageable);

            // Then
            assertEquals(2, result.getContent().size());
            assertEquals(2, result.getTotalElements());
            
            // Verify the repository was called with non-admin parameters
            verify(studentRepository).findByFiltersWithAccess(
                eq(null), 
                eq("%"), 
                eq(false), 
                eq(1L), 
                eq(pageable)
            );
        }
    }

    @Test
    void testGetAllStudents_WithStatusFilter_AdminSeesAllBranches() {
        // Given
        Page<Student> expectedPage = new PageImpl<>(allBranchStudents, pageable, allBranchStudents.size());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(adminUser);
            
            when(studentRepository.findByFiltersWithAccess(
                eq("LEAD"), 
                eq("%"), 
                eq(true), 
                eq(null), 
                eq(pageable)
            )).thenReturn(expectedPage);

            // When
            var result = studentService.getAllStudents(StudentStatus.LEAD, null, pageable);

            // Then
            assertEquals(3, result.getContent().size());
            
            // Verify the repository was called with admin parameters and status
            verify(studentRepository).findByFiltersWithAccess(
                eq("LEAD"), 
                eq("%"), 
                eq(true), 
                eq(null), 
                eq(pageable)
            );
        }
    }

    @Test
    void testGetAllStudents_WithSearchFilter_NonAdminSeesOnlyTheirBranch() {
        // Given
        Page<Student> expectedPage = new PageImpl<>(branch1Students, pageable, branch1Students.size());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(nonAdminUser);
            
            when(studentRepository.findByFiltersWithAccess(
                eq(null), 
                eq("%test%"), 
                eq(false), 
                eq(1L), 
                eq(pageable)
            )).thenReturn(expectedPage);

            // When
            var result = studentService.getAllStudents(null, "test", pageable);

            // Then
            assertEquals(2, result.getContent().size());
            
            // Verify the repository was called with non-admin parameters and search
            verify(studentRepository).findByFiltersWithAccess(
                eq(null), 
                eq("%test%"), 
                eq(false), 
                eq(1L), 
                eq(pageable)
            );
        }
    }

    @Test
    void testGetAllStudents_ReturnsSimplifiedBranchData() {
        // Given
        Branch chennaiBranch = new Branch();
        chennaiBranch.setId(1L);
        chennaiBranch.setName("Chennai");
        chennaiBranch.setLocation("Test Location");

        User user1 = new User();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setEmail("john@test.com");

        Student student1 = new Student();
        student1.setId(1L);
        student1.setUser(user1);
        student1.setBranch(chennaiBranch);
        student1.setEmail("john@test.com");
        student1.setStatus(StudentStatus.LEAD);

        List<Student> students = Arrays.asList(student1);
        Page<Student> expectedPage = new PageImpl<>(students, pageable, students.size());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(adminUser);
            
            when(studentRepository.findByFiltersWithAccess(
                eq(null), 
                eq("%"), 
                eq(true), 
                eq(null), 
                eq(pageable)
            )).thenReturn(expectedPage);

            // When
            var result = studentService.getAllStudents(null, null, pageable);

            // Then
            assertEquals(1, result.getContent().size());
            
            Student response = result.getContent().get(0);
            assertEquals(1L, response.getId());
            assertEquals("John", response.getUser().getFirstName());
            assertEquals("Doe", response.getUser().getLastName());
            assertEquals("john@test.com", response.getEmail());
            
            // Verify branch data is accessible through the branch relationship
            assertEquals(1L, response.getBranch().getId());
            assertEquals("Chennai", response.getBranch().getName());
            
            // Verify the response contains Student objects, not StudentResponse DTOs
            assertTrue(result.getContent().get(0) instanceof Student);
        }
    }
}
