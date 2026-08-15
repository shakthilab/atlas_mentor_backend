package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight document metadata for a student's document list. Deliberately excludes
 * {@link Document#getBase64Content()} so GET /api/students/{id} stays cheap to load even
 * when a student has many/large documents; the actual bytes are only fetched on demand via
 * StudentController's per-document download endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String documentName;
    private String documentType;
    private String fileType;
    private LocalDateTime createdAt;

    public static DocumentResponse fromEntity(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setDocumentName(document.getDocumentName());
        response.setDocumentType(document.getDocumentType());
        response.setFileType(document.getFileType());
        response.setCreatedAt(document.getCreatedAt());
        return response;
    }
}
