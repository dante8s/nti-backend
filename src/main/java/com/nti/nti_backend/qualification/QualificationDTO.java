package com.nti.nti_backend.qualification;

import java.util.List;

public class QualificationDTO {

    public record SubjectDTO(Long id, String subjectName, int position) {}

    public record StackDTO(
            Long id,
            int stackNumber,
            String specializationKey,
            String specializationName,
            List<SubjectDTO> subjects
    ) {}
}
