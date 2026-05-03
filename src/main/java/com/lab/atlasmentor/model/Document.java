package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "documents")
@Data
@EqualsAndHashCode(callSuper = true)
public class Document extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Column(name = "document_name", length = 255)
    private String documentName;
    
    @Column(name = "document_type", length = 100)
    private String documentType;
    
    @Column(name = "base64_content", columnDefinition = "TEXT")
    private String base64Content;
    
    @Column(name = "file_type", length = 50)
    private String fileType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
        
    public Document() {}
    
    public Document(Student student, String documentName, String fileType, User uploadedBy) {
        this.student = student;
        this.documentName = documentName;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;
    }

    
}
