package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "documents")
@Data
public class Document extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;
    
    @Column(name = "file_type", length = 50)
    private String fileType;
    
    @Column(name = "uploaded_by")
    private Long uploadedBy;
    
    public Document() {}
    
    public Document(Long studentId, String fileUrl, String fileType, Long uploadedBy) {
        this.studentId = studentId;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;
    }
    
}
