package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.dto.ApiResponse;
import com.lab.atlasmentor.enums.FinancialAuditAction;
import com.lab.atlasmentor.model.FinancialAuditLog;
import com.lab.atlasmentor.service.FinancialAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit/financial")
public class FinancialAuditController {

    @Autowired
    private FinancialAuditService financialAuditService;

    /** All audit records for a specific entity (e.g. StudentPayment ID 42). */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FinancialAuditLog>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<FinancialAuditLog> records = financialAuditService.getByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(records.isEmpty() ? "No data found" : "Audit records retrieved", records));
    }

    /** All audit records performed by a specific user. */
    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FinancialAuditLog>>> getByActor(
            @PathVariable Long actorId) {
        List<FinancialAuditLog> records = financialAuditService.getByActor(actorId);
        return ResponseEntity.ok(ApiResponse.success(records.isEmpty() ? "No data found" : "Audit records retrieved", records));
    }

    /** All records for a given action type. */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FinancialAuditLog>>> getByAction(
            @PathVariable FinancialAuditAction action) {
        List<FinancialAuditLog> records = financialAuditService.getByAction(action);
        return ResponseEntity.ok(ApiResponse.success(records.isEmpty() ? "No data found" : "Audit records retrieved", records));
    }

    /** All records within a time window. */
    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FinancialAuditLog>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<FinancialAuditLog> records = financialAuditService.getByDateRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(records.isEmpty() ? "No data found" : "Audit records retrieved", records));
    }

    /**
     * Verify the integrity hash of a single audit record.
     * Returns 200 + verified:true if the stored hash matches a freshly computed hash,
     * 200 + verified:false if the record appears to have been tampered with.
     */
    @GetMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> verifyIntegrity(@PathVariable Long id) {
        boolean ok = financialAuditService.verifyIntegrity(id);
        String msg = ok ? "Integrity verified — record is untampered"
                        : "INTEGRITY FAILURE — record has been modified after creation";
        return ResponseEntity.ok(ApiResponse.success(msg, ok));
    }

    /**
     * Bulk integrity check for all records belonging to one entity.
     * Returns the list of record IDs whose hash does not match (empty list = clean).
     */
    @GetMapping("/entity/{entityType}/{entityId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Long>>> verifyEntityIntegrity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<Long> tampered = financialAuditService.findTamperedRecords(entityType, entityId);
        String msg = tampered.isEmpty()
                ? "All records for " + entityType + "/" + entityId + " are untampered"
                : "INTEGRITY FAILURES detected in " + tampered.size() + " record(s)";
        return ResponseEntity.ok(ApiResponse.success(msg, tampered));
    }
}
