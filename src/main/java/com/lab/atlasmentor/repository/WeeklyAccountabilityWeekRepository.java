package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyAccountabilityWeekRepository extends JpaRepository<WeeklyAccountabilityWeek, Long> {
    List<WeeklyAccountabilityWeek> findByTemplateIdOrderByWeekNumberAsc(Long templateId);
    Optional<WeeklyAccountabilityWeek> findByTemplateIdAndWeekNumber(Long templateId, Integer weekNumber);
}
