package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TemplateDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateDayRepository extends JpaRepository<TemplateDay, Long> {
    List<TemplateDay> findByRoleTemplateId(Long roleTemplateId);
}
