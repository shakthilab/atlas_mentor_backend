package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.CommissionTrendResponse;
import com.lab.atlasmentor.dto.CommissionTrendResponse.TrendPoint;
import com.lab.atlasmentor.dto.ReferralSummaryResponse;
import com.lab.atlasmentor.dto.ReferralSummaryResponse.PayoutStats;
import com.lab.atlasmentor.dto.ReferralSummaryResponse.ReferralCounts;
import com.lab.atlasmentor.repository.ClientPayoutRepository;
import com.lab.atlasmentor.repository.ReferralDetailsRepository;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private ReferralDetailsRepository referralDetailsRepository;

    @Autowired
    private ClientPayoutRepository clientPayoutRepository;

    public ReferralSummaryResponse getReferralSummary() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return buildAdminSummary();
        } else if ("MANAGER".equalsIgnoreCase(role) || "BRANCH_PARTNER".equalsIgnoreCase(role)) {
            return buildBranchSummary(currentUser.getBranchId());
        } else {
            // REFERRAL or COMPANY — their own data only
            return buildUserSummary(currentUser.getUserId());
        }
    }

    private ReferralSummaryResponse buildAdminSummary() {
        List<Object[]> byTypeRows = referralDetailsRepository.countGroupByReferralType();
        List<Object[]> byStatusRows = referralDetailsRepository.countGroupByUserStatus();
        List<Object[]> payoutRows = clientPayoutRepository.getPayoutStatsByStatusGlobal();

        ReferralCounts counts = buildReferralCounts(byTypeRows, byStatusRows);
        PayoutStats stats = buildPayoutStats(payoutRows);
        return new ReferralSummaryResponse(counts, stats);
    }

    private ReferralSummaryResponse buildBranchSummary(Long branchId) {
        List<Object[]> byTypeRows = referralDetailsRepository.countGroupByReferralTypeForBranch(branchId);
        List<Object[]> byStatusRows = referralDetailsRepository.countGroupByUserStatusForBranch(branchId);
        List<Object[]> payoutRows = clientPayoutRepository.getPayoutStatsByStatusForBranch(branchId);

        ReferralCounts counts = buildReferralCounts(byTypeRows, byStatusRows);
        PayoutStats stats = buildPayoutStats(payoutRows);
        return new ReferralSummaryResponse(counts, stats);
    }

    private ReferralSummaryResponse buildUserSummary(Long userId) {
        List<Object[]> payoutRows = clientPayoutRepository.getPayoutStatsByStatusForUser(userId);

        Map<String, Long> byType = new LinkedHashMap<>();
        long active = 0;
        long inactive = 0;

        List<Object[]> typeAndStatusRows = referralDetailsRepository.findTypeAndStatusByUserId(userId);
        if (!typeAndStatusRows.isEmpty()) {
            Object[] typeAndStatus = typeAndStatusRows.get(0);
            String referralType = (String) typeAndStatus[0];
            String userStatus = (String) typeAndStatus[1];
            byType.put(referralType, 1L);
            if ("ACTIVE".equalsIgnoreCase(userStatus)) {
                active = 1;
            } else {
                inactive = 1;
            }
        }

        ReferralCounts counts = new ReferralCounts(1L, active, inactive, byType);
        PayoutStats stats = buildPayoutStats(payoutRows);
        return new ReferralSummaryResponse(counts, stats);
    }

    private ReferralCounts buildReferralCounts(List<Object[]> byTypeRows, List<Object[]> byStatusRows) {
        Map<String, Long> byType = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : byTypeRows) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            byType.put(type, count);
            total += count;
        }

        long active = 0;
        long inactive = 0;
        for (Object[] row : byStatusRows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if ("ACTIVE".equalsIgnoreCase(status)) {
                active = count;
            } else {
                inactive += count;
            }
        }

        return new ReferralCounts(total, active, inactive, byType);
    }

    // ==================== COMMISSION TREND ====================

    public CommissionTrendResponse getCommissionTrend(String range, LocalDate from, LocalDate to) {
        LocalDate resolvedFrom;
        LocalDate resolvedTo;
        String rangeLabel;

        if (from != null && to != null) {
            resolvedFrom = from;
            resolvedTo = to;
            rangeLabel = "custom";
        } else {
            resolvedTo = LocalDate.now();
            int days = "15d".equals(range) ? 15 : "30d".equals(range) ? 30 : 7;
            resolvedFrom = resolvedTo.minusDays(days - 1);
            rangeLabel = range != null ? range : "7d";
        }

        long totalDays = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;
        GroupBy groupBy;
        if ("30d".equals(range) && from == null) {
            groupBy = GroupBy.WEEKLY;
        } else if (from != null) {
            if (totalDays <= 31) groupBy = GroupBy.DAILY;
            else if (totalDays <= 90) groupBy = GroupBy.WEEKLY;
            else groupBy = GroupBy.MONTHLY;
        } else {
            groupBy = GroupBy.DAILY;
        }

        List<Object[]> rawData = fetchTrendData(resolvedFrom, resolvedTo);
        Map<LocalDate, BigDecimal[]> dayMap = buildDayMap(rawData);

        List<TrendPoint> points;
        if (groupBy == GroupBy.WEEKLY) {
            points = buildWeeklyPoints(resolvedFrom, resolvedTo, dayMap);
        } else if (groupBy == GroupBy.MONTHLY) {
            points = buildMonthlyPoints(resolvedFrom, resolvedTo, dayMap);
        } else {
            points = buildDailyPoints(resolvedFrom, resolvedTo, dayMap);
        }

        return new CommissionTrendResponse(rangeLabel, resolvedFrom.toString(), resolvedTo.toString(), points);
    }

    private enum GroupBy { DAILY, WEEKLY, MONTHLY }

    private List<Object[]> fetchTrendData(LocalDate from, LocalDate to) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        String role = currentUser.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            return clientPayoutRepository.getTrendDataGlobal(from, to);
        } else if ("MANAGER".equalsIgnoreCase(role) || "BRANCH_PARTNER".equalsIgnoreCase(role)) {
            return clientPayoutRepository.getTrendDataForBranch(currentUser.getBranchId(), from, to);
        } else {
            return clientPayoutRepository.getTrendDataForUser(currentUser.getUserId(), from, to);
        }
    }

    private Map<LocalDate, BigDecimal[]> buildDayMap(List<Object[]> rawData) {
        Map<LocalDate, BigDecimal[]> map = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            LocalDate day;
            if (row[0] instanceof LocalDate) {
                day = (LocalDate) row[0];
            } else {
                day = ((java.sql.Date) row[0]).toLocalDate();
            }
            BigDecimal commission = new BigDecimal(row[1].toString());
            BigDecimal pending = new BigDecimal(row[2].toString());
            map.put(day, new BigDecimal[]{commission, pending});
        }
        return map;
    }

    private List<TrendPoint> buildDailyPoints(LocalDate from, LocalDate to, Map<LocalDate, BigDecimal[]> dayMap) {
        List<TrendPoint> points = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BigDecimal[] vals = dayMap.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            points.add(new TrendPoint(d.format(fmt), vals[0], vals[1]));
        }
        return points;
    }

    private List<TrendPoint> buildWeeklyPoints(LocalDate from, LocalDate to, Map<LocalDate, BigDecimal[]> dayMap) {
        List<TrendPoint> points = new ArrayList<>();
        LocalDate weekStart = from;
        while (!weekStart.isAfter(to)) {
            LocalDate weekEnd = weekStart.plusDays(6).isAfter(to) ? to : weekStart.plusDays(6);
            BigDecimal commission = BigDecimal.ZERO;
            BigDecimal pending = BigDecimal.ZERO;
            for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                BigDecimal[] vals = dayMap.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                commission = commission.add(vals[0]);
                pending = pending.add(vals[1]);
            }
            points.add(new TrendPoint(formatWeekLabel(weekStart, weekEnd), commission, pending));
            weekStart = weekEnd.plusDays(1);
        }
        return points;
    }

    private List<TrendPoint> buildMonthlyPoints(LocalDate from, LocalDate to, Map<LocalDate, BigDecimal[]> dayMap) {
        List<TrendPoint> points = new ArrayList<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");
        LocalDate monthStart = from.withDayOfMonth(1);
        while (!monthStart.isAfter(to)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            LocalDate rangeStart = monthStart.isBefore(from) ? from : monthStart;
            LocalDate rangeEnd = monthEnd.isAfter(to) ? to : monthEnd;
            BigDecimal commission = BigDecimal.ZERO;
            BigDecimal pending = BigDecimal.ZERO;
            for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
                BigDecimal[] vals = dayMap.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                commission = commission.add(vals[0]);
                pending = pending.add(vals[1]);
            }
            points.add(new TrendPoint(monthStart.format(monthFmt), commission, pending));
            monthStart = monthStart.plusMonths(1);
        }
        return points;
    }

    private String formatWeekLabel(LocalDate start, LocalDate end) {
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM d");
        if (start.getMonth() == end.getMonth()) {
            return start.format(DateTimeFormatter.ofPattern("MMM d")) + "–" + end.getDayOfMonth();
        }
        return start.format(dayFmt) + "–" + end.format(dayFmt);
    }

    private PayoutStats buildPayoutStats(List<Object[]> payoutRows) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalDisputed = BigDecimal.ZERO;
        long totalRecords = 0;

        for (Object[] row : payoutRows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal assigned = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            BigDecimal paid = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            BigDecimal disputed = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;

            statusCounts.put(status, count);
            totalRecords += count;
            totalAssigned = totalAssigned.add(assigned);
            totalPaid = totalPaid.add(paid);
            totalDisputed = totalDisputed.add(disputed);
        }

        BigDecimal totalPendingBalance = totalAssigned.subtract(totalPaid);
        return new PayoutStats(totalRecords, statusCounts, totalAssigned, totalPaid, totalPendingBalance, totalDisputed);
    }
}