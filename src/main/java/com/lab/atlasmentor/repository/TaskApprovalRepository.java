package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.TaskApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskApprovalRepository extends JpaRepository<TaskApproval, Long> {
    List<TaskApproval> findByTaskId(Long taskId);
}
