package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.WeeklyAccountabilityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyAccountabilityResponseRepository extends JpaRepository<WeeklyAccountabilityResponse, Long> {
    List<WeeklyAccountabilityResponse> findByEmployeeIdAndCheckpointDate(Long employeeId, LocalDate checkpointDate);
    Optional<WeeklyAccountabilityResponse> findByEmployeeIdAndQuestionIdAndCheckpointDate(
            Long employeeId, Long questionId, LocalDate checkpointDate);
}
