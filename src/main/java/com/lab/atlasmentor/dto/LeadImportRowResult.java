package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class LeadImportRowResult {

    public enum Status { SUCCESS, DUPLICATE, FAILED }

    private int row;
    private Status status;
    private String message;
    private Long studentId;
}
