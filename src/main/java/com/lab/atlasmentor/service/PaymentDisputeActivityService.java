package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.PaymentDisputeActivity;
import com.lab.atlasmentor.model.StudentPayment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.PaymentDisputeActivityRepository;
import com.lab.atlasmentor.enums.PaymentDisputeAction;
import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.StudentPaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentDisputeActivityService {

    @Autowired
    private PaymentDisputeActivityRepository activityRepository;

    @Transactional
    public PaymentDisputeActivity logDisputeInitiated(StudentPayment payment, String reason, User doneBy) {
        Optional<PaymentDisputeActivity> existingActivity = activityRepository.findByPaymentId(payment.getId());

        if (existingActivity.isPresent()) {
            PaymentDisputeActivity activity = existingActivity.get();
            activity.setAction(PaymentDisputeAction.DISPUTE_INITIATED);
            activity.setOldValue(activity.getNewValue());
            activity.setNewValue(StudentPaymentStatus.DISPUTE.name());
            activity.setReason(reason);
            activity.setDoneBy(doneBy);
            activity.setUpdatedAt(LocalDateTime.now());
            activity.setStatus(DisputeStatus.INITIATED);
            return activityRepository.save(activity);
        }

        PaymentDisputeActivity activity = new PaymentDisputeActivity(
            payment,
            PaymentDisputeAction.DISPUTE_INITIATED,
            StudentPaymentStatus.PENDING.name(),
            StudentPaymentStatus.DISPUTE.name(),
            reason,
            doneBy
        );
        return activityRepository.save(activity);
    }

    @Transactional
    public PaymentDisputeActivity logDisputeAccepted(StudentPayment payment, String response, User doneBy) {
        Optional<PaymentDisputeActivity> existingActivity = activityRepository.findByPaymentId(payment.getId());

        if (existingActivity.isPresent()) {
            PaymentDisputeActivity activity = existingActivity.get();
            activity.setAction(PaymentDisputeAction.DISPUTE_ACCEPTED);
            activity.setOldValue(StudentPaymentStatus.DISPUTE.name());
            activity.setNewValue(StudentPaymentStatus.REJECTED.name());
            activity.setReason(response);
            activity.setDoneBy(doneBy);
            activity.setUpdatedAt(LocalDateTime.now());
            activity.setStatus(DisputeStatus.ACCEPTED);
            return activityRepository.save(activity);
        }

        throw new IllegalStateException("No dispute found for payment ID: " + payment.getId());
    }

    @Transactional
    public PaymentDisputeActivity logDisputeRejected(StudentPayment payment, String response, User doneBy) {
        Optional<PaymentDisputeActivity> existingActivity = activityRepository.findByPaymentId(payment.getId());

        if (existingActivity.isPresent()) {
            PaymentDisputeActivity activity = existingActivity.get();
            activity.setAction(PaymentDisputeAction.DISPUTE_REJECTED);
            activity.setOldValue(StudentPaymentStatus.DISPUTE.name());
            activity.setNewValue(StudentPaymentStatus.DISPUTE.name());
            activity.setReason(response);
            activity.setDoneBy(doneBy);
            activity.setUpdatedAt(LocalDateTime.now());
            activity.setStatus(DisputeStatus.REJECTED);
            return activityRepository.save(activity);
        }

        throw new IllegalStateException("No dispute found for payment ID: " + payment.getId());
    }

    @Transactional(readOnly = true)
    public List<PaymentDisputeActivity> getPaymentDisputeActivities(Long paymentId) {
        return activityRepository.findByPaymentIdOrderByDoneAtDesc(paymentId);
    }

    @Transactional(readOnly = true)
    public List<PaymentDisputeActivity> getPaymentDisputeActivities(StudentPayment payment) {
        return activityRepository.findByPaymentOrderByDoneAtDesc(payment);
    }

    @Transactional
    public PaymentDisputeActivity logPaymentAssigned(StudentPayment payment, BigDecimal amount, User doneBy) {
        Optional<PaymentDisputeActivity> existingActivity = activityRepository.findByPaymentId(payment.getId());

        if (existingActivity.isPresent()) {
            PaymentDisputeActivity activity = existingActivity.get();
            activity.setAction(PaymentDisputeAction.PAYMENT_ASSIGNED);
            activity.setOldValue(activity.getNewValue());
            activity.setNewValue(amount.toString());
            activity.setReason("Payment assigned: " + amount);
            activity.setDoneBy(doneBy);
            activity.setUpdatedAt(LocalDateTime.now());
            activity.setStatus(DisputeStatus.INITIATED); // Keep as initiated until resolved
            return activityRepository.save(activity);
        }

        // Create new activity if none exists
        PaymentDisputeActivity activity = new PaymentDisputeActivity(
            payment,
            PaymentDisputeAction.PAYMENT_ASSIGNED,
            StudentPaymentStatus.PENDING.name(),
            amount.toString(),
            "Payment assigned: " + amount,
            doneBy
        );
        activity.setStatus(DisputeStatus.INITIATED);
        return activityRepository.save(activity);
    }
}
