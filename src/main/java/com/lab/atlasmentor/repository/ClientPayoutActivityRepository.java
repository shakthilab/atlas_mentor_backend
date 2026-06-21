package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.enums.ClientPayoutAction;
import com.lab.atlasmentor.enums.DisputeStage;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.ClientPayoutActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientPayoutActivityRepository extends JpaRepository<ClientPayoutActivity, Long> {
    
    // Find by client payout
    List<ClientPayoutActivity> findByClientPayoutIdOrderByDoneAtDesc(Long clientPayoutId);
    
    // Find by client payout and action
    List<ClientPayoutActivity> findByClientPayoutIdAndActionOrderByDoneAtDesc(Long clientPayoutId, ClientPayoutAction action);
    
    // Find by action
    List<ClientPayoutActivity> findByActionOrderByDoneAtDesc(ClientPayoutAction action);
    
    // Find by user
    List<ClientPayoutActivity> findByDoneByOrderByDoneAtDesc(Long doneById);
    
    // Find by date range
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.doneAt BETWEEN :startDate AND :endDate ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findByDoneAtBetweenOrderByDoneAtDesc(@Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);
    
    // Find by user and date range
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.doneBy.id = :userId AND cpa.doneAt BETWEEN :startDate AND :endDate ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findByDoneByIdAndDoneAtBetweenOrderByDoneAtDesc(@Param("userId") Long userId,
                                                                          @Param("startDate") LocalDateTime startDate,
                                                                          @Param("endDate") LocalDateTime endDate);
    
    // Find dispute activities
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.action IN :actions ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findByActionInOrderByDoneAtDesc(@Param("actions") List<ClientPayoutAction> actions);
    
    // Find by dispute stage
    List<ClientPayoutActivity> findByDisputeStageOrderByDoneAtDesc(DisputeStage disputeStage);
    
    // Find payment activities
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.action = :action ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findPaymentActivities(@Param("action") ClientPayoutAction action);
    
    // Count by action
    @Query("SELECT COUNT(cpa) FROM ClientPayoutActivity cpa WHERE cpa.action = :action")
    Long countByAction(@Param("action") ClientPayoutAction action);
    
    // Count by user and action
    @Query("SELECT COUNT(cpa) FROM ClientPayoutActivity cpa WHERE cpa.doneBy.id = :userId AND cpa.action = :action")
    Long countByDoneByIdAndAction(@Param("userId") Long userId, @Param("action") ClientPayoutAction action);
    
    // Find recent activities
    @Query("SELECT cpa FROM ClientPayoutActivity cpa ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findRecentActivities();
    
    // Find recent activities with limit
    @Query("SELECT cpa FROM ClientPayoutActivity cpa ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findRecentActivitiesWithLimit();
    
    // Find activities for reporting
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE " +
           "(:clientPayoutId IS NULL OR cpa.clientPayout.id = :clientPayoutId) AND " +
           "(:action IS NULL OR cpa.action = :action) AND " +
           "(:userId IS NULL OR cpa.doneBy.id = :userId) AND " +
           "(:startDate IS NULL OR cpa.doneAt >= :startDate) AND " +
           "(:endDate IS NULL OR cpa.doneAt <= :endDate) " +
           "ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findActivitiesForReporting(@Param("clientPayoutId") Long clientPayoutId,
                                                     @Param("action") ClientPayoutAction action,
                                                     @Param("userId") Long userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    // Find last activity for payout
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.clientPayout.id = :clientPayoutId ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findLastActivityForPayout(@Param("clientPayoutId") Long clientPayoutId);
    
    // Find amount change activities
    @Query("SELECT cpa FROM ClientPayoutActivity cpa WHERE cpa.action = :action AND cpa.oldAmount IS NOT NULL AND cpa.newAmount IS NOT NULL ORDER BY cpa.doneAt DESC")
    List<ClientPayoutActivity> findAmountChangeActivities(@Param("action") ClientPayoutAction action);

    @Modifying
    @Query("DELETE FROM ClientPayoutActivity cpa WHERE cpa.doneBy.id = :userId")
    void deleteByDoneByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ClientPayoutActivity cpa WHERE cpa.clientPayout.user.id = :userId")
    void deleteByClientPayoutUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ClientPayoutActivity cpa WHERE cpa.clientPayout.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
