package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyAccountabilityQuestionRepository extends JpaRepository<WeeklyAccountabilityQuestion, Long> {
    List<WeeklyAccountabilityQuestion> findByWeekIdOrderByDisplayOrderAsc(Long weekId);
}
