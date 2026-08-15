package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lab.atlasmentor.enums.LeadSource;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeadSourceResponse {

    private Integer id;

    @JsonProperty("enum")
    private String sourceEnum;

    private String displayName;

    public static LeadSourceResponse fromSource(LeadSource source) {
        return new LeadSourceResponse(source.ordinal() + 1, source.name(), source.getDisplayName());
    }
}
