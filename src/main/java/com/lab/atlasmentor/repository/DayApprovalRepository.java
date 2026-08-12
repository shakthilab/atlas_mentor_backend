package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.DayApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DayApprovalRepository extends JpaRepository<DayApproval, Long> {
    List<DayApproval> findByDayWorkspaceId(Long dayWorkspaceId);

    List<DayApproval> findByDayWorkspaceIdOrderByActedAtAsc(Long dayWorkspaceId);
}
