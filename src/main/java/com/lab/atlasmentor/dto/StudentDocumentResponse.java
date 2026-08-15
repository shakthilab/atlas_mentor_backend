package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One student document with its file content base64-encoded - the list form of
 * {@link DocumentDownloadResponse}, returned by GET /api/students/{studentId}/documents so the
 * frontend can render/download every document from one call instead of one request per id.
 * `content` is the same base64 payload the single-document download endpoint returns; it's null
 * (not an error) when the document row has no stored/decodable file content.
 */
@Data
@AllArgsConstructor
public class StudentDocumentResponse {
    private Long id;
    private String documentType;
    private String fileName;
    private String fileType;
    private String content;
}
