package com.lab.atlasmentor.repository;

import com.lab.atlasmentor.model.PaymentDisputeActivity;
import com.lab.atlasmentor.model.StudentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDisputeActivityRepository extends JpaRepository<PaymentDisputeActivity, Long> {

    List<PaymentDisputeActivity> findByPaymentIdOrderByDoneAtDesc(Long paymentId);

    List<PaymentDisputeActivity> findByPaymentOrderByDoneAtDesc(StudentPayment payment);

    @Query("SELECT a FROM PaymentDisputeActivity a WHERE a.payment.id = :paymentId ORDER BY a.doneAt DESC")
    List<PaymentDisputeActivity> findActivitiesByPaymentId(@Param("paymentId") Long paymentId);

    Optional<PaymentDisputeActivity> findByPaymentId(Long paymentId);
}
