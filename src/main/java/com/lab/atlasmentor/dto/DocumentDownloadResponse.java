package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JSON-friendly form of {@link DocumentDownload} returned by
 * StudentController#downloadStudentDocument. The file bytes are base64-encoded so the frontend
 * can decode `content` client-side (e.g. `atob` / a data URL built from `fileType` + `content`)
 * instead of receiving a raw binary response.
 */
@Data
@AllArgsConstructor
public class DocumentDownloadResponse {
    private String fileName;
    private String fileType;
    private String content;
}
