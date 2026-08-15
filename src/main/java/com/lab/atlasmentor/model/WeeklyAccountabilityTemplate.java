package com.lab.atlasmentor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.BundleStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

/**
 * Role- AND cycle-month-scoped template for the Weekly Accountability checkpoint - same
 * DRAFT/ACTIVE/INACTIVE publish lifecycle as {@link TaskBundle} (Role Templates), but
 * deliberately its own table rather than an extension of task_bundles. A template covers
 * exactly one calendar month ({@link #cycleMonth}, always stored as the 1st of that month)
 * for one role, broken into 4 {@link WeeklyAccountabilityWeek} slots, each independently
 * authored - not a flat question list, and never feeds {@link Task} generation. Reuses
 * {@link BundleStatus} for the status column rather than introducing a parallel enum: DRAFT is
 * left only via publish and never re-entered, and ACTIVE/INACTIVE toggle back and forth via
 * {@link com.lab.atlasmentor.service.WeeklyAccountabilityTemplateService#updateTemplateStatus}.
 */
@Entity
@Table(name = "weekly_accountability_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeeklyAccountabilityTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_role_id", nullable = false)
    @JsonIgnoreProperties({"users"})
    private Role targetRole;

    /** Always the 1st of the month - see chk_weekly_acct_templates_cycle_month_first_of_month. */
    @Column(name = "cycle_month", nullable = false)
    private LocalDate cycleMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BundleStatus status = BundleStatus.DRAFT;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"template"})
    private List<WeeklyAccountabilityWeek> weeks;

    public WeeklyAccountabilityTemplate() {}

    public Long getTargetRoleId() {
        return targetRole != null ? targetRole.getId() : null;
    }
}
