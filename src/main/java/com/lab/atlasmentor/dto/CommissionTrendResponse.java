package com.lab.atlasmentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTrendResponse {

    private String range;
    private String from;
    private String to;
    private List<TrendPoint> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private BigDecimal commissionReceived;
        private BigDecimal pendingBalance;
    }
}