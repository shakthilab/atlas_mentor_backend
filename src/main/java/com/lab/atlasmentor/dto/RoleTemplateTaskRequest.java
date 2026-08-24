package com.lab.atlasmentor.dto;

import lombok.Data;
import com.lab.atlasmentor.enums.Priority;

import java.util.List;

/**
 * Dual-purpose request body for POST/PUT .../days/{dayNumber}/tasks:
 *
 * - Single-task mode (default, backward compatible): populate title/description/priority
 *   directly, exactly as before this class supported bulk mode. Title is required in this mode
 *   - enforced in RoleTemplateService rather than via @NotBlank here, since that annotation
 *   would otherwise also demand a (meaningless) top-level title on bulk requests.
 * - Bulk mode (POST only): populate `tasks` instead - one call clones every entry onto the
 *   day in the URL, plus any extra days listed in `targetDays` (e.g. cloning Aug day 23's 10
 *   tasks onto Sep days 1-5 in a single atomic request instead of 50 individual POSTs). When
 *   `tasks` is non-empty, the top-level title/description/priority/id fields are ignored.
 */
@Data
public class RoleTemplateTaskRequest {
    private Long id;

    private String title;

    private String description;

    private Priority priority = Priority.MEDIUM;

    private Integer displayOrder;

    /** Bulk mode: one or more tasks to add. Non-empty here switches the whole request to bulk mode. */
    private List<RoleTemplateTaskRequest> tasks;

    /** Bulk mode: extra days (besides the URL's own dayNumber/month/year) to clone `tasks` onto. */
    private List<RoleTemplateTaskTargetDayRequest> targetDays;
}
