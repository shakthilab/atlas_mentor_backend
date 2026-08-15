package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.BundleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body for the single activate/deactivate endpoint - mirrors {@link RoleTemplateStatusRequest}.
 * {@code status} carries the desired target state. Only ACTIVE and INACTIVE are valid here -
 * DRAFT is entered automatically at creation and left only via the dedicated /publish
 * endpoint, never set directly.
 */
@Data
public class WeeklyAccountabilityTemplateStatusRequest {
    @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
    private BundleStatus status;
}
