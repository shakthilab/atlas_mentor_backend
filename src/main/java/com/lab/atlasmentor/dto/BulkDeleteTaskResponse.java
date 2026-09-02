package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteTaskResponse {

    private int deletedCount;
    private List<Long> deletedTaskIds;
    private List<Long> failedTaskIds;
}
