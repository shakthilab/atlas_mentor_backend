package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * File bytes + metadata needed to stream a single document back to the client, returned by
 * StudentService#downloadDocument(Long). Not a JSON response DTO - StudentController writes
 * these fields straight into the HTTP response (Content-Type, Content-Disposition, body).
 */
@Data
@AllArgsConstructor
public class DocumentDownload {
    private String fileName;
    private String fileType;
    private byte[] content;
}
