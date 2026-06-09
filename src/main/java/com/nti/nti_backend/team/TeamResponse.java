package com.nti.nti_backend.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long leaderId;
    private Integer maxCapacity;
    private String description;
    private String competencies;
    private List<TeamMemberResponse> members;
}