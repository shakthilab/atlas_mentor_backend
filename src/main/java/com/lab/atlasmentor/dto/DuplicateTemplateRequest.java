package com.lab.atlasmentor.dto;

import lombok.Data;

/**
 * newTemplateName is optional. When blank/omitted, the service defaults it to
 * "<source template name> Copy" (e.g. duplicating "Senior Counsellor Aug" produces
 * "Senior Counsellor Aug Copy") so duplication never fails just for a missing name -
 * the admin can still rename the copy afterwards via the normal update endpoint.
 */
@Data
public class DuplicateTemplateRequest {
    private String newTemplateName;
}
