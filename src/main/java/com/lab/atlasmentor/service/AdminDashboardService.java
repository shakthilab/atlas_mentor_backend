package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.model.FinancialAuditLog;
import com.lab.atlasmentor.model.StudentActivity;
import com.lab.atlasmentor.repository.*;
import com.lab.atlasmentor.security.CustomUserDetails;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    @Autowired private StudentRepository studentRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private StudentPaymentRepository studentPaymentRepository;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired private ClientPayoutRepository clientPayoutRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private FinancialAuditLogRepository financialAuditLogRepository;
    @Autowired private StudentStatusApprovalRepository studentStatusApprovalRepository;
    @Autowired private StudentActivityRepository studentActivityRepository;

    private static final String[] PALETTE = {
        "#0d6efd", "#20c997", "#fd7e14", "#dc3545", "#6f42c1", "#ffc107", "#17a2b8", "#198754"
    };

    // ==================== PERIOD & ROLE HELPERS ====================

    private LocalDateTime resolveFromDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        if (period == null) return now.minusDays(7);
        switch (period.toLowerCase()) {
            case "7d":  return now.minusDays(7);
            case "15d": return now.minusDays(15);
            case "30d": return now.minusDays(30);
            case "60d": return now.minusDays(60);
            case "90d": return now.minusDays(90);
            case "1y":  return now.minusYears(1);
            case "5y":  return now.minusYears(5);
            default:    return now.minusDays(7);
        }
    }

    private LocalDateTime resolvePrevFromDate(LocalDateTime from) {
        long days = ChronoUnit.DAYS.between(from, LocalDateTime.now());
        return from.minusDays(Math.max(days, 1));
    }

    private Long resolvedBranchId() {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        String role = user.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) return null;
        return user.getBranchId();
    }

    private Object[] getFirst(List<Object[]> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private double computeChangePercent(long previous, long current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((current - previous) * 100.0 / previous * 10.0) / 10.0;
    }

    private List<Double> buildMonthlyTrend(List<Object[]> rows) {
        List<Double> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(((Number) row[1]).doubleValue());
        }
        return result;
    }

    private int computeHealthScore(long activeStudents, long totalStudents) {
        if (totalStudents == 0) return 50;
        double ratio = activeStudents * 100.0 / totalStudents;
        return (int) Math.min(100, Math.round(40 + ratio * 0.6));
    }

    // ==================== FORMAT HELPERS ====================

    private String color(int index) {
        return PALETTE[Math.abs(index) % PALETTE.length];
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "₹0";
        double val = amount.doubleValue();
        if (Math.abs(val) >= 100000) return String.format("₹%.1fL", val / 100000);
        if (Math.abs(val) >= 1000)   return String.format("₹%.1fK", val / 1000);
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatTrend(double change) {
        if (change >= 0) return String.format("↗ +%.1f%%", change);
        return String.format("↘ %.1f%%", change);
    }

    private String trendColor(double change) {
        return change >= 0 ? "#198754" : "#dc3545";
    }

    private String priorityColor(String priority) {
        if ("HIGH".equalsIgnoreCase(priority))   return "#dc3545";
        if ("MEDIUM".equalsIgnoreCase(priority)) return "#fd7e14";
        if ("LOW".equalsIgnoreCase(priority))    return "#198754";
        return "#6c757d";
    }

    private String healthColor(int score) {
        if (score >= 80) return "#198754";
        if (score >= 50) return "#fd7e14";
        return "#dc3545";
    }

    private String actionClass(String action) {
        if ("UPDATE".equalsIgnoreCase(action)) return "warning";
        if ("CREATE".equalsIgnoreCase(action)) return "success";
        if ("DELETE".equalsIgnoreCase(action)) return "danger";
        return "secondary";
    }

    private String toRgba(String hex, double alpha) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("rgba(%d,%d,%d,%.1f)", r, g, b, alpha);
        } catch (Exception e) {
            return "rgba(0,0,0,0.1)";
        }
    }

    private String formatUpperTimeAgo(LocalDateTime dt) {
        if (dt == null) return "";
        long minutes = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (minutes < 1) return "JUST NOW";
        if (minutes < 60) return minutes + "M AGO";
        long hours = minutes / 60;
        if (hours < 24) return hours + "H AGO";
        return (hours / 24) + "D AGO";
    }

    private String formatUpperTimeAgoFromStr(String timestamp) {
        try {
            String normalized = timestamp.replace(" ", "T");
            // strip timezone offset or fractional seconds beyond seconds
            int tPos = normalized.indexOf('T');
            if (tPos >= 0) {
                String datePart = normalized.substring(0, tPos);
                String timePart = normalized.substring(tPos + 1);
                // keep only HH:mm:ss
                if (timePart.length() > 8) timePart = timePart.substring(0, 8);
                normalized = datePart + "T" + timePart;
            }
            LocalDateTime dt = LocalDateTime.parse(normalized);
            return formatUpperTimeAgo(dt);
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== SECTION 01 – KPI CARDS ====================

    public DashboardKpiResponse getKpiCards(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime prevFrom = resolvePrevFromDate(from);

        long totalStudents = branchId == null
                ? studentRepository.count()
                : Optional.ofNullable(studentRepository.countByBranchId(branchId)).orElse(0L);
        long newThisPeriod = branchId == null
                ? Optional.ofNullable(studentRepository.countNewBetween(from, now)).orElse(0L)
                : Optional.ofNullable(studentRepository.countNewBetweenForBranch(branchId, from, now)).orElse(0L);
        long newPrevPeriod = branchId == null
                ? Optional.ofNullable(studentRepository.countNewBetween(prevFrom, from)).orElse(0L)
                : Optional.ofNullable(studentRepository.countNewBetweenForBranch(branchId, prevFrom, from)).orElse(0L);
        double newStudentsChange = computeChangePercent(newPrevPeriod, newThisPeriod);
        List<Double> studentTrend = buildMonthlyTrend(branchId == null
                ? studentRepository.countMonthlyNewStudents(from)
                : studentRepository.countMonthlyNewStudentsForBranch(branchId, from));

        long activeTasks = branchId == null
                ? Optional.ofNullable(taskRepository.countActiveTasks()).orElse(0L)
                : Optional.ofNullable(taskRepository.countActiveTasksForBranch(branchId)).orElse(0L);
        long overdueTasks = branchId == null
                ? Optional.ofNullable(taskRepository.countOverdueTasks()).orElse(0L)
                : Optional.ofNullable(taskRepository.countOverdueTasksForBranch(branchId)).orElse(0L);

        Object[] finSummary = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(from))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, from));
        BigDecimal totalRevenue = finSummary != null && finSummary[1] != null
                ? new BigDecimal(finSummary[1].toString()) : BigDecimal.ZERO;

        Object[] finPrev = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(prevFrom))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, prevFrom));
        BigDecimal prevRevenue = finPrev != null && finPrev[1] != null
                ? new BigDecimal(finPrev[1].toString()) : BigDecimal.ZERO;
        double revenueChange = computeChangePercent(prevRevenue.longValue(), totalRevenue.longValue());

        BigDecimal pendingPayouts = branchId == null
                ? Optional.ofNullable(clientPayoutRepository.getTotalPendingPayouts(from)).orElse(BigDecimal.ZERO)
                : Optional.ofNullable(clientPayoutRepository.getTotalPendingPayoutsForBranch(branchId, from)).orElse(BigDecimal.ZERO);

        long activeEmployees = branchId == null
                ? Optional.ofNullable(userRepository.countActiveEmployees()).orElse(0L)
                : Optional.ofNullable(userRepository.countActiveEmployeesForBranch(branchId)).orElse(0L);
        long lastMonthEmp = Optional.ofNullable(userRepository.countActiveEmployeesLastMonth()).orElse(0L);
        double employeeChange = computeChangePercent(lastMonthEmp, activeEmployees);

        long activeBranches = branchId == null
                ? Optional.ofNullable(branchRepository.countActiveBranches()).orElse(0L)
                : 1L;

        List<DashboardKpiResponse.SummaryCard> summaryCards = new ArrayList<>();
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Total Students").value(String.format("%,d", totalStudents))
                .trend(formatTrend(newStudentsChange)).trendColor(trendColor(newStudentsChange))
                .prefix("").suffix("").chartData(studentTrend).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("New This Period").value(String.format("%,d", newThisPeriod))
                .trend(formatTrend(newStudentsChange)).trendColor(trendColor(newStudentsChange))
                .prefix("").suffix("").chartData(studentTrend).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Active Tasks").value(String.format("%,d", activeTasks))
                .trend("").trendColor("")
                .prefix("").suffix("").chartData(Collections.emptyList()).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Overdue Tasks").value(String.format("%,d", overdueTasks))
                .trend(overdueTasks > 0 ? "Critical" : "Clear")
                .trendColor(overdueTasks > 0 ? "#dc3545" : "#198754")
                .prefix("").suffix("").chartData(Collections.emptyList()).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Total Revenue").value(formatCurrency(totalRevenue))
                .trend(formatTrend(revenueChange)).trendColor(trendColor(revenueChange))
                .prefix("").suffix("").chartData(Collections.emptyList()).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Pending Payouts").value(formatCurrency(pendingPayouts))
                .trend(pendingPayouts.compareTo(BigDecimal.ZERO) > 0 ? "Review" : "Clear")
                .trendColor(pendingPayouts.compareTo(BigDecimal.ZERO) > 0 ? "#fd7e14" : "#198754")
                .prefix("").suffix("").chartData(Collections.emptyList()).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Active Employees").value(String.format("%,d", activeEmployees))
                .trend(formatTrend(employeeChange)).trendColor(trendColor(employeeChange))
                .prefix("").suffix("").chartData(Collections.emptyList()).build());
        summaryCards.add(DashboardKpiResponse.SummaryCard.builder()
                .title("Active Branches").value(String.format("%,d", activeBranches))
                .trend("Stable").trendColor("#198754")
                .prefix("").suffix("").chartData(Collections.emptyList()).build());

        return DashboardKpiResponse.builder().summaryCards(summaryCards).build();
    }

    // ==================== SECTION 02 – STUDENTS ====================

    public DashboardStudentsResponse getStudentsSection(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);

        // 1. Status breakdown (enhanced)
        List<Object[]> enhancedRows = branchId == null
                ? studentRepository.countGroupByEnhancedStatus(from)
                : studentRepository.countGroupByEnhancedStatusForBranch(branchId, from);
        long enhancedTotal = enhancedRows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<DashboardStudentsResponse.StatusBreakdown> statusBreakdown = new ArrayList<>();
        for (int i = 0; i < enhancedRows.size(); i++) {
            Object[] row = enhancedRows.get(i);
            long cnt = ((Number) row[1]).longValue();
            int pct = enhancedTotal > 0 ? (int) Math.round(cnt * 100.0 / enhancedTotal) : 0;
            statusBreakdown.add(DashboardStudentsResponse.StatusBreakdown.builder()
                    .status(String.valueOf(row[0])).count(cnt).percentage(pct).color(color(i)).build());
        }

        // 2. Top countries
        List<Object[]> countryRows = branchId == null
                ? studentRepository.findTopCountries(from)
                : studentRepository.findTopCountriesForBranch(branchId, from);
        List<DashboardStudentsResponse.TopCountry> topCountries = new ArrayList<>();
        for (int i = 0; i < countryRows.size(); i++) {
            Object[] row = countryRows.get(i);
            long students      = ((Number) row[1]).longValue(); // new registrations in period
            long convertedAll  = ((Number) row[2]).longValue(); // all-time status=STUDENT
            long totalAll      = ((Number) row[3]).longValue(); // all-time total students
            int convRate = totalAll > 0 ? (int) Math.round(convertedAll * 100.0 / totalAll) : 0;
            topCountries.add(DashboardStudentsResponse.TopCountry.builder()
                    .rank(i + 1).country(String.valueOf(row[0]))
                    .students(students).converted(convertedAll)
                    .conversionRate(convRate + "%").build());
        }

        // 3. Acquisition funnel (basic status)
        List<Object[]> statusRows = branchId == null
                ? studentRepository.countGroupByStatus(from)
                : studentRepository.countGroupByStatusForBranch(branchId, from);
        List<DashboardStudentsResponse.FunnelItem> acquisitionFunnel = new ArrayList<>();
        for (int i = 0; i < statusRows.size(); i++) {
            Object[] row = statusRows.get(i);
            acquisitionFunnel.add(DashboardStudentsResponse.FunnelItem.builder()
                    .stage(String.valueOf(row[0]))
                    .count(((Number) row[1]).longValue())
                    .color(color(i)).build());
        }

        // 4. Referral / Company / Direct pie
        List<Object[]> sourceRows = branchId == null
                ? studentRepository.countGroupBySourceType(from)
                : studentRepository.countGroupBySourceTypeForBranch(branchId, from);
        long referralCount = 0, companyCount = 0;
        for (Object[] row : sourceRows) {
            String src = String.valueOf(row[0]);
            long cnt = ((Number) row[1]).longValue();
            if ("REFERRAL".equalsIgnoreCase(src)) referralCount = cnt;
            else if ("COMPANY".equalsIgnoreCase(src)) companyCount = cnt;
        }
        long totalStudents = branchId == null
                ? studentRepository.count()
                : Optional.ofNullable(studentRepository.countByBranchId(branchId)).orElse(0L);
        long directCount = Math.max(0, totalStudents - referralCount - companyCount);
        List<DashboardStudentsResponse.PieItem> referralCompany = new ArrayList<>();
        referralCompany.add(DashboardStudentsResponse.PieItem.builder().name("Referral").value(referralCount)
                .itemStyle(DashboardStudentsResponse.ItemStyle.builder().color(color(0)).build()).build());
        referralCompany.add(DashboardStudentsResponse.PieItem.builder().name("Company").value(companyCount)
                .itemStyle(DashboardStudentsResponse.ItemStyle.builder().color(color(1)).build()).build());
        referralCompany.add(DashboardStudentsResponse.PieItem.builder().name("Direct").value(directCount)
                .itemStyle(DashboardStudentsResponse.ItemStyle.builder().color(color(2)).build()).build());

        // 5. Intake country series
        List<String> topCountryNames = topCountries.stream()
                .limit(4).map(DashboardStudentsResponse.TopCountry::getCountry)
                .collect(Collectors.toList());
        List<String> intakeLabels = new ArrayList<>();
        List<DashboardStudentsResponse.ChartSeries> intakeCountrySeries = new ArrayList<>();
        if (!topCountryNames.isEmpty()) {
            List<Object[]> intakeRows = branchId == null
                    ? studentRepository.findMonthlyIntakeByCountry(from, topCountryNames)
                    : studentRepository.findMonthlyIntakeByCountryForBranch(branchId, from, topCountryNames);
            Set<String> monthSet = new LinkedHashSet<>();
            Map<String, Map<String, Long>> countryMonthData = new LinkedHashMap<>();
            for (Object[] row : intakeRows) {
                String month = String.valueOf(row[0]);
                String country = String.valueOf(row[1]);
                long cnt = ((Number) row[2]).longValue();
                monthSet.add(month);
                countryMonthData.computeIfAbsent(country, k -> new LinkedHashMap<>()).put(month, cnt);
            }
            intakeLabels = new ArrayList<>(monthSet);
            for (int i = 0; i < topCountryNames.size(); i++) {
                String country = topCountryNames.get(i);
                Map<String, Long> monthData = countryMonthData.getOrDefault(country, Collections.emptyMap());
                List<Long> data = new ArrayList<>();
                for (String month : intakeLabels) {
                    data.add(monthData.getOrDefault(month, 0L));
                }
                intakeCountrySeries.add(DashboardStudentsResponse.ChartSeries.builder()
                        .name(country).type("bar").stack("intake").barWidth("40%")
                        .data(data)
                        .itemStyle(DashboardStudentsResponse.ItemStyle.builder().color(color(i)).build())
                        .build());
            }
        }

        return DashboardStudentsResponse.builder()
                .statusBreakdown(statusBreakdown).topCountries(topCountries)
                .acquisitionFunnel(acquisitionFunnel).referralCompany(referralCompany)
                .intakeLabels(intakeLabels).intakeCountrySeries(intakeCountrySeries)
                .build();
    }

    // ==================== SECTION 03 – FINANCIAL ====================

    public DashboardFinancialResponse getFinancialSection(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);
        LocalDateTime prevFrom = resolvePrevFromDate(from);

        Object[] fin = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(from))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, from));
        BigDecimal totalBilled = fin != null && fin[0] != null ? new BigDecimal(fin[0].toString()) : BigDecimal.ZERO;
        BigDecimal totalPaid   = fin != null && fin[1] != null ? new BigDecimal(fin[1].toString()) : BigDecimal.ZERO;

        Object[] finPrev = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(prevFrom))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, prevFrom));
        BigDecimal prevBilled = finPrev != null && finPrev[0] != null ? new BigDecimal(finPrev[0].toString()) : BigDecimal.ZERO;
        BigDecimal prevPaid   = finPrev != null && finPrev[1] != null ? new BigDecimal(finPrev[1].toString()) : BigDecimal.ZERO;

        Object[] disputeData = branchId == null
                ? getFirst(clientPayoutRepository.getDisputeSummary(from))
                : getFirst(clientPayoutRepository.getDisputeSummaryForBranch(branchId, from));
        BigDecimal inDispute = disputeData != null && disputeData[1] != null
                ? new BigDecimal(disputeData[1].toString()) : BigDecimal.ZERO;
        BigDecimal pending = totalBilled.subtract(totalPaid).subtract(inDispute).max(BigDecimal.ZERO);

        double billedChange = computeChangePercent(prevBilled.longValue(), totalBilled.longValue());
        double paidChange   = computeChangePercent(prevPaid.longValue(), totalPaid.longValue());

        List<DashboardFinancialResponse.MetricCard> financialMetrics = Arrays.asList(
            DashboardFinancialResponse.MetricCard.builder()
                .title("Total Billed").value(formatCurrency(totalBilled))
                .trend(formatTrend(billedChange)).trendColor(trendColor(billedChange)).build(),
            DashboardFinancialResponse.MetricCard.builder()
                .title("Total Paid").value(formatCurrency(totalPaid))
                .trend(formatTrend(paidChange)).trendColor(trendColor(paidChange)).build(),
            DashboardFinancialResponse.MetricCard.builder()
                .title("Pending").value(formatCurrency(pending))
                .trend("").trendColor("").build(),
            DashboardFinancialResponse.MetricCard.builder()
                .title("In Dispute").value(formatCurrency(inDispute))
                .trend("").trendColor("").build()
        );

        // Top paying entities
        List<DashboardFinancialResponse.PayingEntity> topPayingEntities = new ArrayList<>();
        List<Object[]> recRows = branchId == null
                ? clientPayoutRepository.findTopReceivables(from)
                : clientPayoutRepository.findTopReceivablesForBranch(branchId, from);
        for (Object[] row : recRows) {
            topPayingEntities.add(DashboardFinancialResponse.PayingEntity.builder()
                    .entity(String.valueOf(row[0])).type(String.valueOf(row[1]))
                    .amount(formatCurrency(row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO))
                    .status(String.valueOf(row[3]))
                    .lastActivity(row[4] != null ? formatUpperTimeAgoFromStr(row[4].toString()) : "")
                    .build());
        }

        // Open disputes
        List<DashboardFinancialResponse.DisputeItem> openDisputes = new ArrayList<>();
        List<Object[]> dispRows = branchId == null
                ? clientPayoutRepository.findOpenDisputes(from)
                : clientPayoutRepository.findOpenDisputesForBranch(branchId, from);
        for (Object[] row : dispRows) {
            openDisputes.add(DashboardFinancialResponse.DisputeItem.builder()
                    .id(((Number) row[0]).longValue())
                    .entityName(String.valueOf(row[1]))
                    .reason(row[2] != null ? String.valueOf(row[2]) : "")
                    .amount(formatCurrency(row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO))
                    .disputedAt(row[4] != null ? formatUpperTimeAgoFromStr(row[4].toString()) : "")
                    .build());
        }

        // Payment method distribution
        List<Object[]> methodRows = branchId == null
                ? paymentTransactionRepository.getPaymentMethodDistribution(from)
                : paymentTransactionRepository.getPaymentMethodDistributionForBranch(branchId, from);
        BigDecimal grandTotal = methodRows.stream().filter(r -> r[2] != null)
                .map(r -> new BigDecimal(r[2].toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DashboardFinancialResponse.PaymentDistLegend> paymentDistLegends = new ArrayList<>();
        List<DashboardFinancialResponse.PieItem> paymentDistribution = new ArrayList<>();
        for (int i = 0; i < methodRows.size(); i++) {
            Object[] row = methodRows.get(i);
            BigDecimal amount = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            int pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue() : 0;
            String c = color(i);
            paymentDistLegends.add(DashboardFinancialResponse.PaymentDistLegend.builder()
                    .method(String.valueOf(row[0]))
                    .count(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                    .totalAmount(formatCurrency(amount)).percentage(pct).color(c).build());
            paymentDistribution.add(DashboardFinancialResponse.PieItem.builder()
                    .name(String.valueOf(row[0])).value(amount.doubleValue())
                    .itemStyle(DashboardFinancialResponse.ItemStyle.builder().color(c).build()).build());
        }

        // Monthly payment breakdown → ECharts bar series
        List<Object[]> breakdownRows = branchId == null
                ? studentPaymentRepository.getMonthlyPaymentBreakdown(from)
                : studentPaymentRepository.getMonthlyPaymentBreakdownForBranch(branchId, from);
        Set<String> monthSet = new LinkedHashSet<>();
        Map<String, Map<String, BigDecimal>> monthStatusData = new LinkedHashMap<>();
        for (Object[] row : breakdownRows) {
            String month = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            BigDecimal amount = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            monthSet.add(month);
            monthStatusData.computeIfAbsent(month, k -> new LinkedHashMap<>()).put(status, amount);
        }
        List<String> paymentsLabels = new ArrayList<>(monthSet);
        String[][] statusColorMap = {
            {"PAID", "#198754"}, {"PENDING", "#fd7e14"}, {"PARTIAL", "#ffc107"}, {"DISPUTE", "#dc3545"}
        };
        List<DashboardFinancialResponse.ChartSeries> paymentsStatusSeries = new ArrayList<>();
        for (String[] sc : statusColorMap) {
            String stat = sc[0];
            String col = sc[1];
            List<Double> data = new ArrayList<>();
            for (String month : paymentsLabels) {
                Map<String, BigDecimal> statuses = monthStatusData.getOrDefault(month, Collections.emptyMap());
                data.add(statuses.getOrDefault(stat, BigDecimal.ZERO).doubleValue());
            }
            if (data.stream().anyMatch(v -> v > 0)) {
                paymentsStatusSeries.add(DashboardFinancialResponse.ChartSeries.builder()
                        .name(stat).type("bar").stack("payments").barWidth("40%")
                        .data(data)
                        .itemStyle(DashboardFinancialResponse.ItemStyle.builder().color(col).build())
                        .build());
            }
        }

        return DashboardFinancialResponse.builder()
                .financialMetrics(financialMetrics).topPayingEntities(topPayingEntities)
                .openDisputes(openDisputes).paymentDistLegends(paymentDistLegends)
                .paymentsLabels(paymentsLabels).paymentsStatusSeries(paymentsStatusSeries)
                .paymentDistribution(paymentDistribution).build();
    }

    // ==================== SECTION 04 – TASKS ====================

    public DashboardTasksResponse getTasksSection(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);

        // Overdue tasks
        List<DashboardTasksResponse.OverdueTask> overdueTasks = new ArrayList<>();
        List<Object[]> overdueRows = branchId == null
                ? taskRepository.findOverdueTasksWithAssignee()
                : taskRepository.findOverdueTasksWithAssigneeForBranch(branchId);
        for (Object[] row : overdueRows) {
            String priority = String.valueOf(row[5]);
            String first = row[2] != null ? String.valueOf(row[2]) : "";
            String last  = row[3] != null ? String.valueOf(row[3]) : "";
            overdueTasks.add(DashboardTasksResponse.OverdueTask.builder()
                    .taskId(((Number) row[0]).longValue())
                    .title(String.valueOf(row[1]))
                    .assignee((first + " " + last).trim())
                    .dueDate(row[4] != null ? row[4].toString() : "")
                    .priority(priority)
                    .daysLate(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                    .priorityColor(priorityColor(priority)).build());
        }

        // Priority heatmap
        List<DashboardTasksResponse.HeatmapCell> priorityHeatmap = new ArrayList<>();
        List<Object[]> heatRows = branchId == null
                ? taskRepository.countGroupByStatusAndPriority(from)
                : taskRepository.countGroupByStatusAndPriorityForBranch(branchId, from);
        for (Object[] row : heatRows) {
            String priority = String.valueOf(row[1]);
            priorityHeatmap.add(DashboardTasksResponse.HeatmapCell.builder()
                    .status(String.valueOf(row[0])).priority(priority)
                    .count(((Number) row[2]).longValue()).color(priorityColor(priority)).build());
        }

        // Throughput series
        Map<String, Long> createdByWeek = new LinkedHashMap<>();
        for (Object[] row : branchId == null
                ? taskRepository.countWeeklyCreatedTasks(from)
                : taskRepository.countWeeklyCreatedTasksForBranch(branchId, from)) {
            createdByWeek.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        Map<String, Long> completedByWeek = new LinkedHashMap<>();
        for (Object[] row : branchId == null
                ? taskRepository.countWeeklyCompletedTasks(from)
                : taskRepository.countWeeklyCompletedTasksForBranch(branchId, from)) {
            completedByWeek.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        Set<String> allWeeks = new TreeSet<>();
        allWeeks.addAll(createdByWeek.keySet());
        allWeeks.addAll(completedByWeek.keySet());
        List<String> throughputLabels = new ArrayList<>();
        List<Long> createdData = new ArrayList<>();
        List<Long> completedData = new ArrayList<>();
        int weekNum = 1;
        for (String week : allWeeks) {
            throughputLabels.add("W" + weekNum++);
            createdData.add(createdByWeek.getOrDefault(week, 0L));
            completedData.add(completedByWeek.getOrDefault(week, 0L));
        }
        List<DashboardTasksResponse.ChartSeries> throughputSeries = Arrays.asList(
            DashboardTasksResponse.ChartSeries.builder()
                    .name("Created").type("line").smooth(true).showSymbol(false).symbolSize(6)
                    .data(createdData)
                    .itemStyle(DashboardTasksResponse.ItemStyle.builder().color("#0d6efd").build())
                    .areaStyle(DashboardTasksResponse.AreaStyle.builder().color(toRgba("#0d6efd", 0.1)).build())
                    .build(),
            DashboardTasksResponse.ChartSeries.builder()
                    .name("Completed").type("line").smooth(true).showSymbol(false).symbolSize(6)
                    .data(completedData)
                    .itemStyle(DashboardTasksResponse.ItemStyle.builder().color("#198754").build())
                    .areaStyle(DashboardTasksResponse.AreaStyle.builder().color(toRgba("#198754", 0.1)).build())
                    .build()
        );

        return DashboardTasksResponse.builder()
                .overdueTasks(overdueTasks).priorityHeatmap(priorityHeatmap)
                .throughputLabels(throughputLabels).throughputSeries(throughputSeries).build();
    }

    // ==================== SECTION 05 – TEAM & BRANCHES ====================

    public DashboardTeamResponse getTeamSection(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);

        // Workload
        List<DashboardTeamResponse.WorkloadItem> workload = new ArrayList<>();
        List<Object[]> workRows = branchId == null
                ? taskRepository.findEmployeeTaskLoad(from)
                : taskRepository.findEmployeeTaskLoadForBranch(branchId, from);
        for (int i = 0; i < workRows.size(); i++) {
            Object[] row = workRows.get(i);
            workload.add(DashboardTeamResponse.WorkloadItem.builder()
                    .userId(((Number) row[0]).longValue())
                    .name(String.valueOf(row[1]))
                    .totalTasks(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                    .overdueTasks(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .barColor(color(i)).build());
        }

        // Role breakdown (pie)
        List<DashboardTeamResponse.PieItem> roleBreakdown = new ArrayList<>();
        List<Object[]> roleRows = branchId == null
                ? userRepository.countActiveEmployeesByRole()
                : userRepository.countActiveEmployeesByRoleForBranch(branchId);
        for (int i = 0; i < roleRows.size(); i++) {
            Object[] row = roleRows.get(i);
            roleBreakdown.add(DashboardTeamResponse.PieItem.builder()
                    .name(String.valueOf(row[0])).value(((Number) row[1]).longValue())
                    .itemStyle(DashboardTeamResponse.ItemStyle.builder().color(color(i)).build()).build());
        }

        // Leaderboard
        List<DashboardTeamResponse.LeaderboardEntry> leaderboard = new ArrayList<>();
        List<Object[]> lbRows = branchId == null
                ? userRepository.findEmployeeLeaderboard(from)
                : userRepository.findEmployeeLeaderboardForBranch(branchId, from);
        int rank = 1;
        for (Object[] row : lbRows) {
            BigDecimal revenue = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            leaderboard.add(DashboardTeamResponse.LeaderboardEntry.builder()
                    .rank(rank++).userId(((Number) row[0]).longValue())
                    .name(String.valueOf(row[1]))
                    .branch(row[2] != null ? String.valueOf(row[2]) : "")
                    .revenue(formatCurrency(revenue)).revenueRaw(revenue.doubleValue()).build());
        }

        // Pending approvals
        Object[] disputeForBranch = branchId == null ? null
                : getFirst(clientPayoutRepository.getDisputeSummaryForBranch(branchId, from));
        long unresolvedDisputes = branchId == null
                ? Optional.ofNullable(clientPayoutRepository.countByPayoutStatus(
                        com.lab.atlasmentor.enums.ClientPayoutStatus.DISPUTE)).orElse(0L)
                : (disputeForBranch != null && disputeForBranch[0] != null
                        ? ((Number) disputeForBranch[0]).longValue() : 0L);
        long payoutsAwaiting = branchId == null
                ? Optional.ofNullable(clientPayoutRepository.countPayoutsAwaitingApproval()).orElse(0L)
                : Optional.ofNullable(clientPayoutRepository.countPayoutsAwaitingApprovalForBranch(branchId)).orElse(0L);
        long pendingStudents = Optional.ofNullable(studentStatusApprovalRepository.countAllPending()).orElse(0L);
        long auditAlerts = financialAuditLogRepository.findRecentLogs(from).size();

        List<DashboardTeamResponse.ApprovalItem> pendingApprovals = Arrays.asList(
            DashboardTeamResponse.ApprovalItem.builder()
                    .label("Unresolved Disputes").count(unresolvedDisputes).badgeClass("danger").build(),
            DashboardTeamResponse.ApprovalItem.builder()
                    .label("Payouts Awaiting Approval").count(payoutsAwaiting).badgeClass("warning").build(),
            DashboardTeamResponse.ApprovalItem.builder()
                    .label("Pending Student Approvals").count(pendingStudents).badgeClass("info").build(),
            DashboardTeamResponse.ApprovalItem.builder()
                    .label("Audit Alerts").count(auditAlerts).badgeClass("secondary").build()
        );

        // Branch performance
        List<DashboardTeamResponse.BranchPerformance> branchPerf = new ArrayList<>();
        List<Object[]> bpRows = branchId == null
                ? branchRepository.findBranchPerformance()
                : branchRepository.findBranchPerformance().stream()
                        .filter(r -> branchId.equals(((Number) r[0]).longValue()))
                        .collect(Collectors.toList());
        for (Object[] row : bpRows) {
            long total  = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long active = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            BigDecimal revenue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
            int health = computeHealthScore(active, total);
            branchPerf.add(DashboardTeamResponse.BranchPerformance.builder()
                    .branchId(((Number) row[0]).longValue()).branch(String.valueOf(row[1]))
                    .totalStudents(total).activeStudents(active)
                    .revenue(formatCurrency(revenue)).revenueRaw(revenue.doubleValue())
                    .tasks(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                    .team(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                    .healthScore(health).healthColor(healthColor(health)).build());
        }

        return DashboardTeamResponse.builder()
                .workload(workload).roleBreakdown(roleBreakdown)
                .leaderboard(leaderboard).pendingApprovals(pendingApprovals)
                .branchPerformance(branchPerf).build();
    }

    // ==================== SECTION 06 – REFERRALS & PARTNERS ====================

    public DashboardReferralsResponse getReferralsSection(String period) {
        Long branchId = resolvedBranchId();
        LocalDateTime from = resolveFromDate(period);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime prevFrom = resolvePrevFromDate(from);

        Object[] funnelData = branchId == null
                ? getFirst(clientPayoutRepository.getReferralFunnel(from))
                : getFirst(clientPayoutRepository.getReferralFunnelForBranch(branchId, from));
        long referred     = funnelData != null && funnelData[0] != null ? ((Number) funnelData[0]).longValue() : 0L;
        long lead         = funnelData != null && funnelData[1] != null ? ((Number) funnelData[1]).longValue() : 0L;
        long registered   = funnelData != null && funnelData[2] != null ? ((Number) funnelData[2]).longValue() : 0L;
        long activeStudent= funnelData != null && funnelData[3] != null ? ((Number) funnelData[3]).longValue() : 0L;
        long paid         = funnelData != null && funnelData[4] != null ? ((Number) funnelData[4]).longValue() : 0L;

        Object[] fin = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(from))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, from));
        BigDecimal totalPaid = fin != null && fin[1] != null ? new BigDecimal(fin[1].toString()) : BigDecimal.ZERO;

        Object[] finPrev = branchId == null
                ? getFirst(studentPaymentRepository.getFinancialSummary(prevFrom))
                : getFirst(studentPaymentRepository.getFinancialSummaryForBranch(branchId, prevFrom));
        BigDecimal prevPaid = finPrev != null && finPrev[1] != null ? new BigDecimal(finPrev[1].toString()) : BigDecimal.ZERO;
        double commissionMoM = computeChangePercent(prevPaid.longValue(), totalPaid.longValue());

        long newThisPeriod = branchId == null
                ? Optional.ofNullable(studentRepository.countNewBetween(from, now)).orElse(0L)
                : Optional.ofNullable(studentRepository.countNewBetweenForBranch(branchId, from, now)).orElse(0L);

        BigDecimal avgPerReferral = referred > 0
                ? totalPaid.divide(BigDecimal.valueOf(referred), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardReferralsResponse.ReferralMetric> referralMetrics = Arrays.asList(
            DashboardReferralsResponse.ReferralMetric.builder()
                    .title("Total Referrals").value(String.format("%,d", referred))
                    .trend("").trendColor("").build(),
            DashboardReferralsResponse.ReferralMetric.builder()
                    .title("Commission Earned").value(formatCurrency(totalPaid))
                    .trend(formatTrend(commissionMoM)).trendColor(trendColor(commissionMoM)).build(),
            DashboardReferralsResponse.ReferralMetric.builder()
                    .title("Avg Per Referral").value(formatCurrency(avgPerReferral))
                    .trend("").trendColor("").build(),
            DashboardReferralsResponse.ReferralMetric.builder()
                    .title("New This Period").value(String.format("%,d", newThisPeriod))
                    .trend("").trendColor("").build()
        );

        String[] stageNames  = {"Referred", "Lead", "Registered", "Active", "Paid"};
        long[]   stageCounts = {referred, lead, registered, activeStudent, paid};
        List<DashboardReferralsResponse.FunnelItem> referralFunnel = new ArrayList<>();
        for (int i = 0; i < stageNames.length; i++) {
            referralFunnel.add(DashboardReferralsResponse.FunnelItem.builder()
                    .stage(stageNames[i]).count(stageCounts[i]).color(color(i)).build());
        }

        List<Object[]> refRows = branchId == null
                ? clientPayoutRepository.findTopReferrers(from)
                : clientPayoutRepository.findTopReferrersForBranch(branchId, from);
        BigDecimal totalCommission = refRows.stream().filter(r -> r[3] != null)
                .map(r -> new BigDecimal(r[3].toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DashboardReferralsResponse.TopReferrer> topReferrers = new ArrayList<>();
        int refRank = 1;
        for (Object[] row : refRows) {
            BigDecimal commission = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            double share = totalCommission.compareTo(BigDecimal.ZERO) > 0
                    ? commission.divide(totalCommission, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;
            topReferrers.add(DashboardReferralsResponse.TopReferrer.builder()
                    .rank(refRank++).name(String.valueOf(row[0])).partnerType(String.valueOf(row[1]))
                    .students(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                    .commission(formatCurrency(commission)).commissionRaw(commission.doubleValue())
                    .sharePercent(Math.round(share * 10.0) / 10.0).build());
        }

        List<Object[]> earnRows = branchId == null
                ? clientPayoutRepository.getMonthlyEarningsByPartnerType(from)
                : clientPayoutRepository.getMonthlyEarningsByPartnerTypeForBranch(branchId, from);
        Set<String> earnMonthSet = new LinkedHashSet<>();
        Set<String> typeSet = new LinkedHashSet<>();
        Map<String, Map<String, BigDecimal>> earnData = new LinkedHashMap<>();
        for (Object[] row : earnRows) {
            String month = String.valueOf(row[0]);
            String type  = String.valueOf(row[1]);
            BigDecimal earnings = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            earnMonthSet.add(month);
            typeSet.add(type);
            earnData.computeIfAbsent(type, k -> new LinkedHashMap<>()).put(month, earnings);
        }
        List<String> earningsLabels = new ArrayList<>(earnMonthSet);
        List<DashboardReferralsResponse.ChartSeries> earningsPartnerSeries = new ArrayList<>();
        int typeIdx = 0;
        for (String type : typeSet) {
            Map<String, BigDecimal> monthData = earnData.getOrDefault(type, Collections.emptyMap());
            List<Double> data = new ArrayList<>();
            for (String month : earningsLabels) {
                data.add(monthData.getOrDefault(month, BigDecimal.ZERO).doubleValue());
            }
            earningsPartnerSeries.add(DashboardReferralsResponse.ChartSeries.builder()
                    .name(type).type("bar").stack("earnings").barWidth("40%")
                    .data(data)
                    .itemStyle(DashboardReferralsResponse.ItemStyle.builder().color(color(typeIdx++)).build())
                    .build());
        }

        return DashboardReferralsResponse.builder()
                .referralMetrics(referralMetrics).referralFunnel(referralFunnel)
                .topReferrers(topReferrers).earningsLabels(earningsLabels)
                .earningsPartnerSeries(earningsPartnerSeries).build();
    }

    // ==================== SECTION 07 – AUDIT & ACTIVITY ====================

    public DashboardAuditResponse getAuditSection(String period) {
        LocalDateTime from = resolveFromDate(period);

        List<DashboardAuditResponse.AuditEntry> auditLog = new ArrayList<>();
        for (FinancialAuditLog log : financialAuditLogRepository.findRecentLogs(from)) {
            String action = log.getAction().name();
            auditLog.add(DashboardAuditResponse.AuditEntry.builder()
                    .id(log.getId())
                    .time(log.getOccurredAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .action(action)
                    .entityType(log.getEntityType())
                    .entityId(String.valueOf(log.getEntityId()))
                    .actor(String.valueOf(log.getActorId()))
                    .change((log.getOldValue() != null ? log.getOldValue() : "—") + " → " +
                            (log.getNewValue() != null ? log.getNewValue() : "—"))
                    .actionClass(actionClass(action))
                    .changeClass("fw-bold text-dark").build());
        }

        List<DashboardAuditResponse.ActivityItem> activityFeed = new ArrayList<>();
        List<StudentActivity> activities = studentActivityRepository
                .findByPerformedAtAfterOrderByPerformedAtDesc(from);
        int idx = 0;
        for (StudentActivity act : activities.stream().limit(20).collect(Collectors.toList())) {
            String name = act.getStudent() != null && act.getStudent().getName() != null
                    ? act.getStudent().getName() : "Student";
            activityFeed.add(DashboardAuditResponse.ActivityItem.builder()
                    .type(act.getAction().name())
                    .description(name + " — status changed to " + act.getAction().name())
                    .timeAgo(formatUpperTimeAgo(act.getPerformedAt()))
                    .dotColor(color(idx++ % PALETTE.length)).build());
        }

        return DashboardAuditResponse.builder().auditLog(auditLog).activityFeed(activityFeed).build();
    }
}
