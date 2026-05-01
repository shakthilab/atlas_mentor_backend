package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "student_academic_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentAcademicHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Column(name = "qualification", length = 100)
    private String qualification;
    
    @Column(name = "institution_name", length = 255)
    private String institutionName;
    
    @Column(name = "board_university", length = 255)
    private String boardUniversity;
    
    @Column(name = "passing_year")
    private Integer passingYear;
    
    @Column(name = "score", length = 50)
    private String score;
    
    @Column(name = "stream", length = 100)
    private String stream;
    
    public StudentAcademicHistory() {}
    
    public StudentAcademicHistory(Student student, String qualification, String institutionName, 
                                  String boardUniversity, Integer passingYear, String score, String stream) {
        this.student = student;
        this.qualification = qualification;
        this.institutionName = institutionName;
        this.boardUniversity = boardUniversity;
        this.passingYear = passingYear;
        this.score = score;
        this.stream = stream;
    }
}
