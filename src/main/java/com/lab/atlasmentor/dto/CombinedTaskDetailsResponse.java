package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class CombinedTaskDetailsResponse {
    private TaskResponse task;
    private List<TaskCommentResponse> comments;
    private List<ActivityResponse> activities;
}
