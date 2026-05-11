package com.lab.atlasmentor.enums;

/**
 * Enum representing the status of a task bundle.
 * Controls whether the bundle is active for automatic task generation.
 */
public enum BundleStatus {
    /**
     * Bundle is active and will generate tasks according to schedule
     */
    ACTIVE,
    
    /**
     * Bundle is inactive and will not generate tasks
     */
    INACTIVE
}
