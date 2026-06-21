package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.PartnerDashboardResponse;
import com.lab.atlasmentor.enums.ClientPayoutStatus;
import com.lab.atlasmentor.model.ClientPayout;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.ClientPayoutRepository;
import com.lab.atlasmentor.repository.ReferralResourceRepository;
import com.lab.atlasmentor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartnerDashboardService {

    @Autowired private ClientPayoutRepository clientPayoutRepository;
    @Autowired private ReferralResourceRepository referralResourceRepository;
    @Autowired private UserRepository userRepository;

    private static final Locale INDIA = new Locale("en", "IN");

    // ==================== PERIOD HELPERS ====================

    private LocalDateTime resolveFromDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        if (period == null) return now.minusDays(30);
        switch (period.toLowerCase()) {
            case "7d":  return now.minusDays(7);
            case "15d": return now.minusDays(15);
            case "30d": return now.minusDays(30);
            case "60d": return now.minusDays(60);
            case "90d": return now.minusDays(90);
            case "1y":  return now.minusYears(1);
            case "5y":  return now.minusYears(5);
            default:    return now.minusDays(30);
        }
    }

    private LocalDateTime resolvePrevFromDate(LocalDateTime from) {
        long days = ChronoUnit.DAYS.between(from, LocalDateTime.now());
        return from.minusDays(Math.max(days, 1));
    }

    // ==================== FORMAT HELPERS ====================

    private String formatTrend(double change) {
        if (change >= 0) return String.format("+%.0f%%", change);
        return String.format("%.0f%%", change);
    }

    private String trendColor(double change) {
        return change >= 0 ? "#20c997" : "#dc3545";
    }

    private double computeChangePercent(double previous, double current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((current - previous) * 100.0 / previous);
    }

    private String formatAmountValue(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "0";
        double val = amount.doubleValue();
        if (Math.abs(val) >= 100000) return String.format("%.1f", val / 100000);
        if (Math.abs(val) >= 1000)   return String.format("%.1f", val / 1000);
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatAmountSuffix(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "";
        double val = amount.doubleValue();
        if (Math.abs(val) >= 100000) return "L";
        if (Math.abs(val) >= 1000)   return "K";
        return "";
    }

    private String formatIndianCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "₹0";
        long val = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        try {
            return "₹" + NumberFormat.getIntegerInstance(INDIA).format(val);
        } catch (Exception e) {
            return "₹" + String.format("%,d", val);
        }
    }

    private String formatEnrolledDate(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private String resolveDisplayStatus(ClientPayoutStatus status) {
        if (status == null) return "PENDING";
        switch (status) {
            case PAID:            return "PAID";
            case PARTIAL_PAID:    return "PARTIAL";
            case DISPUTE:         return "DISPUTE";
            case AMOUNT_ASSIGNED: return "ASSIGNED";
            case PENDING:         return "PENDING";
            case ACCEPTED:        return "PAID";
            case REJECTED:        return "DISPUTE";
            default:              return status.name();
        }
    }

    // ==================== MAIN SERVICE METHOD ====================

    public PartnerDashboardResponse getDashboard(Long partnerId, String period) {
        LocalDateTime from    = resolveFromDate(period);
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime prevFrom = resolvePrevFromDate(from);

        return PartnerDashboardResponse.builder()
                .summary(buildSummary(partnerId, from, prevFrom, now))
                .earningsOverview(buildEarningsOverview(partnerId, from))
                .payoutStatus(buildPayoutStatus(partnerId))
                .recentStudents(buildRecentStudents(partnerId))
                .quickStats(buildQuickStats(partnerId))
                .build();
    }

    // ==================== SECTION 1: Summary KPI Cards ====================

    private PartnerDashboardResponse.Summary buildSummary(Long partnerId,
                                                           LocalDateTime from,
                                                           LocalDateTime prevFrom,
                                                           LocalDateTime now) {
        // Current period
        List<Object[]> currRows = clientPayoutRepository.getAmountSummaryForUser(partnerId, from);
        BigDecimal currAssigned = BigDecimal.ZERO, currPaid = BigDecimal.ZERO;
        if (currRows != null && !currRows.isEmpty() && currRows.get(0) != null) {
            Object[] r = currRows.get(0);
            if (r[0] != null) currAssigned = new BigDecimal(r[0].toString());
            if (r[1] != null) currPaid     = new BigDecimal(r[1].toString());
        }
        BigDecimal currPending = currAssigned.subtract(currPaid).max(BigDecimal.ZERO);

        // Previous period
        List<Object[]> prevRows = clientPayoutRepository.getAmountSummaryForUser(partnerId, prevFrom);
        BigDecimal prevAssigned = BigDecimal.ZERO, prevPaid = BigDecimal.ZERO;
        if (prevRows != null && !prevRows.isEmpty() && prevRows.get(0) != null) {
            Object[] r = prevRows.get(0);
            if (r[0] != null) prevAssigned = new BigDecimal(r[0].toString());
            if (r[1] != null) prevPaid     = new BigDecimal(r[1].toString());
        }
        BigDecimal prevPending = prevAssigned.subtract(prevPaid).max(BigDecimal.ZERO);

        // Student counts
        long currStudents = Optional.ofNullable(
                clientPayoutRepository.countStudentsByUserIdBetween(partnerId, from, now)).orElse(0L);
        long prevStudents = Optional.ofNullable(
                clientPayoutRepository.countStudentsByUserIdBetween(partnerId, prevFrom, from)).orElse(0L);

        double studentsChange = computeChangePercent(prevStudents, currStudents);
        double assignedChange = computeChangePercent(prevAssigned.doubleValue(), currAssigned.doubleValue());
        double paidChange     = computeChangePercent(prevPaid.doubleValue(), currPaid.doubleValue());
        double pendingChange  = computeChangePercent(prevPending.doubleValue(), currPending.doubleValue());

        return PartnerDashboardResponse.Summary.builder()
                .studentsReferred(PartnerDashboardResponse.KpiCard.builder()
                        .value(String.valueOf(currStudents)).prefix("").suffix("")
                        .trend(formatTrend(studentsChange)).trendColor(trendColor(studentsChange))
                        .build())
                .assignedAmount(PartnerDashboardResponse.KpiCard.builder()
                        .value(formatAmountValue(currAssigned)).prefix("₹").suffix(formatAmountSuffix(currAssigned))
                        .trend(formatTrend(assignedChange)).trendColor(trendColor(assignedChange))
                        .build())
                .paidAmount(PartnerDashboardResponse.KpiCard.builder()
                        .value(formatAmountValue(currPaid)).prefix("₹").suffix(formatAmountSuffix(currPaid))
                        .trend(formatTrend(paidChange)).trendColor(trendColor(paidChange))
                        .build())
                .pendingBalance(PartnerDashboardResponse.KpiCard.builder()
                        .value(formatAmountValue(currPending)).prefix("₹").suffix(formatAmountSuffix(currPending))
                        .trend(formatTrend(pendingChange)).trendColor(trendColor(pendingChange))
                        .build())
                .build();
    }

    // ==================== SECTION 2: Earnings Overview Line Chart ====================

    private PartnerDashboardResponse.EarningsOverview buildEarningsOverview(Long partnerId, LocalDateTime from) {
        List<Object[]> rows = clientPayoutRepository.getMonthlyEarningsForUser(partnerId, from);

        List<String> labels   = new ArrayList<>();
        List<Double> assigned = new ArrayList<>();
        List<Double> paid     = new ArrayList<>();

        for (Object[] row : rows) {
            labels.add(String.valueOf(row[0]));
            assigned.add(row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0.0);
            paid.add(row[2] != null ? new BigDecimal(row[2].toString()).doubleValue() : 0.0);
        }

        return PartnerDashboardResponse.EarningsOverview.builder()
                .labels(labels)
                .datasets(PartnerDashboardResponse.EarningsDatasets.builder()
                        .assigned(assigned).paid(paid).build())
                .build();
    }

    // ==================== SECTION 3: Payout Status Donut Chart ====================

    private PartnerDashboardResponse.PayoutStatusSection buildPayoutStatus(Long partnerId) {
        List<Object[]> rows = clientPayoutRepository.getPayoutStatsByStatusForUser(partnerId);

        long totalPayouts = 0, assignedCount = 0, paidCount = 0,
             pendingCount = 0, partialCount = 0, disputeCount = 0;

        for (Object[] row : rows) {
            String status = String.valueOf(row[0]);
            long count    = ((Number) row[1]).longValue();
            totalPayouts += count;
            switch (status.toUpperCase()) {
                case "AMOUNT_ASSIGNED": assignedCount += count; break;
                case "PAID":            paidCount     += count; break;
                case "PENDING":         pendingCount  += count; break;
                case "PARTIAL_PAID":    partialCount  += count; break;
                case "DISPUTE":         disputeCount  += count; break;
                default: break;
            }
        }

        return PartnerDashboardResponse.PayoutStatusSection.builder()
                .totalPayouts(totalPayouts)
                .distribution(PartnerDashboardResponse.PayoutDistribution.builder()
                        .assigned(assignedCount).paid(paidCount)
                        .pending(pendingCount).partial(partialCount).dispute(disputeCount)
                        .build())
                .build();
    }

    // ==================== SECTION 4: Recent Students Table ====================

    private List<PartnerDashboardResponse.RecentStudent> buildRecentStudents(Long partnerId) {
        List<ClientPayout> payouts = clientPayoutRepository.findByUserIdOrderByCreatedAtDesc(partnerId);

        return payouts.stream().limit(5).map(cp -> {
            String name = (cp.getStudent() != null && cp.getStudent().getName() != null)
                    ? cp.getStudent().getName() : "Unknown";
            String enrolled = cp.getCreatedAt() != null
                    ? formatEnrolledDate(cp.getCreatedAt()) : "";

            BigDecimal assignedAmt = cp.getAssignedAmount() != null ? cp.getAssignedAmount() : BigDecimal.ZERO;
            BigDecimal paidAmt     = cp.getPaidAmount()     != null ? cp.getPaidAmount()     : BigDecimal.ZERO;

            int progress = 0;
            if (assignedAmt.compareTo(BigDecimal.ZERO) > 0) {
                progress = paidAmt.divide(assignedAmt, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).intValue();
            }

            return PartnerDashboardResponse.RecentStudent.builder()
                    .name(name)
                    .enrolled(enrolled)
                    .status(resolveDisplayStatus(cp.getPayoutStatus()))
                    .amount(formatIndianCurrency(assignedAmt))
                    .progress(Math.min(progress, 100))
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== SECTION 5: Quick Stats Sidebar ====================

    private PartnerDashboardResponse.QuickStats buildQuickStats(Long partnerId) {
        long activeResources = referralResourceRepository.countByOwner_IdAndIsActiveTrue(partnerId);

        long openDisputes = Optional.ofNullable(
                clientPayoutRepository.countByUserIdAndPayoutStatus(partnerId, ClientPayoutStatus.DISPUTE))
                .orElse(0L);

        String partnerSince     = "";
        String assignedManager  = null;

        Optional<User> userOpt = userRepository.findByIdWithManager(partnerId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getCreatedAt() != null) {
                partnerSince = user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy"));
            }
            if (user.getReportingManager() != null) {
                assignedManager = user.getReportingManager().getFullName();
            }
        }

        return PartnerDashboardResponse.QuickStats.builder()
                .activeResources(activeResources)
                .openDisputes(openDisputes)
                .partnerSince(partnerSince)
                .assignedManager(assignedManager)
                .build();
    }
}
