package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.WeeklyAccountabilityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeeklyAccountabilityAssignmentRepository extends JpaRepository<WeeklyAccountabilityAssignment, Long> {

    // "Template_Id" (underscore) form, same reason as WeeklyAccountabilityTemplateRepository's
    // TargetRole_Id queries: forces explicit traversal into the template association's id
    // rather than binding to the entity's own getTemplateId() convenience getter.
    Optional<WeeklyAccountabilityAssignment> findByTemplate_Id(Long templateId);
}
