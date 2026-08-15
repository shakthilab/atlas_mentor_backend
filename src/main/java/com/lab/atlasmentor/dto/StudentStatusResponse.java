package com.lab.atlasmentor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lab.atlasmentor.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentStatusResponse {

    private Integer id;

    @JsonProperty("enum")
    private String statusEnum;

    private String displayName;

    public static StudentStatusResponse fromStatus(StudentStatus status) {
        return new StudentStatusResponse(status.ordinal() + 1, status.name(), status.getDisplayName());
    }
}
