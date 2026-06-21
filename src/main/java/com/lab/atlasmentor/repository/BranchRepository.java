package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    
    Optional<Branch> findByName(String name);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Branch b SET b.manager = null WHERE b.manager.id = :userId")
    void nullifyManagerByUserId(@Param("userId") Long userId);

    // ==================== DASHBOARD QUERIES ====================

    @Query(value = "SELECT COUNT(*) FROM branches WHERE status = 'ACTIVE'", nativeQuery = true)
    Long countActiveBranches();

    @Query(value = "SELECT b.id, b.name, " +
           "COUNT(DISTINCT s.id) as total_students, " +
           "COUNT(DISTINCT CASE WHEN s.enhanced_status = 'ACTIVE' THEN s.id END) as active_students, " +
           "COALESCE(SUM(sp.paid_amount), 0) as revenue, " +
           "COUNT(DISTINCT t.id) as tasks_count, " +
           "COUNT(DISTINCT u.id) as team_count " +
           "FROM branches b " +
           "LEFT JOIN students s ON s.branch_id = b.id " +
           "LEFT JOIN student_payments sp ON sp.student_id = s.id AND sp.is_deleted = false " +
           "LEFT JOIN tasks t ON t.branch_id = b.id AND t.is_deleted = false " +
           "LEFT JOIN users u ON u.branch_id = b.id AND u.status = 'ACTIVE' " +
           "GROUP BY b.id, b.name ORDER BY revenue DESC", nativeQuery = true)
    List<Object[]> findBranchPerformance();
}
