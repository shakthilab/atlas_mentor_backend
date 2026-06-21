package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskPageWithStatsResponse {
    private TaskStatsResponse stats;
    private Page<TaskResponse> tasks;
}
