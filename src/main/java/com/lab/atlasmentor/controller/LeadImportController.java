package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.dto.LeadImportFieldDto;
import com.lab.atlasmentor.dto.LeadImportLinkRequest;
import com.lab.atlasmentor.dto.LeadImportResponse;
import com.lab.atlasmentor.enums.LeadImportField;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.service.LeadImportService;
import com.lab.atlasmentor.service.LeadTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lead import: template download (Part 1), file upload (Part 2 Path A), and smart-link
 * import (Part 2 Path B) — see LeadImportField / LeadImportService for the shared field
 * definitions and orchestration.
 */
@RestController
@RequestMapping("/api/leads/import")
public class LeadImportController {

    private final LeadTemplateService templateService;
    private final LeadImportService importService;

    public LeadImportController(LeadTemplateService templateService, LeadImportService importService) {
        this.templateService = templateService;
        this.importService = importService;
    }

    /**
     * Column metadata for the frontend's Import Data modal — read straight off
     * {@link LeadImportField}, the same enum {@link #downloadTemplate()} and the
     * /file, /link parsers already use, so this list can never drift from what the
     * template actually contains or the parser actually validates.
     */
    @GetMapping("/fields")
    public ResponseEntity<ApiResponse<List<LeadImportFieldDto>>> importFields() {
        List<LeadImportFieldDto> fields = Arrays.stream(LeadImportField.ordered())
                .map(field -> new LeadImportFieldDto(field.getHeader(), field.isRequired()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = templateService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lead_import_template.xlsx\"")
                .body(bytes);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LeadImportResponse>> importFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No file was uploaded."));
        }
        try {
            LeadImportResponse response = importService.importFromFile(file);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Import complete", response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/link")
    public ResponseEntity<ApiResponse<LeadImportResponse>> importLink(@RequestBody LeadImportLinkRequest request) {
        if (request.getLink() == null || request.getLink().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("A url is required."));
        }
        try {
            LeadImportResponse response = importService.importFromLink(request.getLink().trim());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Import complete", response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
