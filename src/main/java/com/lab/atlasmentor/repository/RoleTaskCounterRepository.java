package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.RoleTaskCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RoleTaskCounterRepository extends JpaRepository<RoleTaskCounter, Long> {

    Optional<RoleTaskCounter> findByRoleId(Long roleId);

    // Deliberately NOT @Modifying: INSERT ... RETURNING produces a result set, and @Modifying
    // routes native queries through JDBC executeUpdate(), which the Postgres driver rejects
    // for any statement that returns rows ("A result was returned when none was expected").
    // Leaving this as a plain (non-modifying) native query makes Spring Data run it via
    // executeQuery()/getSingleResult() instead, which RETURNING needs - @Transactional is
    // still required since it's a write.
    @Transactional
    @Query(value = """
        INSERT INTO role_task_counters (role_id, last_sequence_number)
        VALUES (:roleId, 1)
        ON CONFLICT (role_id)
        DO UPDATE SET last_sequence_number = role_task_counters.last_sequence_number + 1
        RETURNING last_sequence_number
        """, nativeQuery = true)
    Long incrementAndGetNextSequence(@Param("roleId") Long roleId);
}
