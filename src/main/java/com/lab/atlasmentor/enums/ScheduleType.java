package com.lab.atlasmentor.enums;

/**
 * Enum representing the scheduling type for task bundles.
 * Determines how frequently tasks should be generated from bundles.
 */
public enum ScheduleType {
    /**
     * Tasks generated every day
     */
    DAILY,
    
    /**
     * Tasks generated every week
     */
    WEEKLY,
    
    /**
     * Tasks generated every month
     */
    MONTHLY,
    
    /**
     * Tasks generated only once
     */
    ONE_TIME
}
