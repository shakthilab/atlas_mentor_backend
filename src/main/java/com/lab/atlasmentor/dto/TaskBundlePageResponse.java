package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TaskBundlePageResponse {

    private List<TaskBundleListResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private long totalCount;
    private long activeCount;
    private long inactiveCount;
}