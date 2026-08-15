package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.model.StudentAcademicHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Academic history entry for a student (one per qualification, e.g. 10th/12th/Bachelor's),
 * used to populate StudentResponse#academicHistory in GET /api/students/{id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicHistoryResponse {

    private Long id;
    private String qualification;
    private String institutionName;
    private String boardUniversity;
    private Integer passingYear;
    private String score;
    private String stream;

    public static AcademicHistoryResponse fromEntity(StudentAcademicHistory history) {
        AcademicHistoryResponse response = new AcademicHistoryResponse();
        response.setId(history.getId());
        response.setQualification(history.getQualification());
        response.setInstitutionName(history.getInstitutionName());
        response.setBoardUniversity(history.getBoardUniversity());
        response.setPassingYear(history.getPassingYear());
        response.setScore(history.getScore());
        response.setStream(history.getStream());
        return response;
    }
}
