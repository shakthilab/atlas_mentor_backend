package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyAccountabilityTemplateRepository extends JpaRepository<WeeklyAccountabilityTemplate, Long> {

    // NOTE: the "TargetRole_Id" (underscore) form is required, not stylistic - the entity's
    // own getTargetRoleId() convenience getter makes Spring Data's property-path resolver see
    // a same-named bean property "targetRoleId" that ISN'T a real JPA attribute. Without the
    // underscore forcing explicit traversal into the targetRole association's id, Spring Data
    // binds directly to that bogus property and Hibernate blows up at query time with
    // UnknownPathException ("Could not resolve attribute 'targetRoleId'") - this only surfaces
    // on a real DB call, never in a Mockito-mocked repository test.
    List<WeeklyAccountabilityTemplate> findByTargetRole_Id(Long roleId);
    List<WeeklyAccountabilityTemplate> findByStatus(BundleStatus status);
    List<WeeklyAccountabilityTemplate> findByTargetRole_IdAndStatus(Long roleId, BundleStatus status);
    List<WeeklyAccountabilityTemplate> findByTargetRole_IdAndCycleMonth(Long roleId, LocalDate cycleMonth);

    /** At most one row given uk_weekly_acct_templates_active_role_month (partial unique index on ACTIVE). */
    Optional<WeeklyAccountabilityTemplate> findFirstByTargetRole_IdAndCycleMonthAndStatus(
            Long roleId, LocalDate cycleMonth, BundleStatus status);
}
