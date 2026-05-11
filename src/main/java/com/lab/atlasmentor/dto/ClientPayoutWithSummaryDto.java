package com.lab.atlasmentor.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientPayoutWithSummaryDto {
    
    private List<ClientPayoutDto> payouts;
    private ClientPayoutSummaryDto summary;
    
}
