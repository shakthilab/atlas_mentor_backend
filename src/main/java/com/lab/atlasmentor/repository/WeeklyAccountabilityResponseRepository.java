package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.WeeklyAccountabilityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyAccountabilityResponseRepository extends JpaRepository<WeeklyAccountabilityResponse, Long> {
    List<WeeklyAccountabilityResponse> findByEmployeeId(Long employeeId);
    List<WeeklyAccountabilityResponse> findByDayWorkspaceId(Long dayWorkspaceId);
}
