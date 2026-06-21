package com.lab.atlasmentor.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAuditResponse {
    private List<AuditEntry> auditLog;
    private List<ActivityItem> activityFeed;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AuditEntry {
        private Long id;
        private String time;
        private String action;
        private String entityType;
        private String entityId;
        private String actor;
        private String change;
        private String actionClass;
        private String changeClass;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ActivityItem {
        private String type;
        private String description;
        private String timeAgo;
        private String dotColor;
    }
}
