package com.nti.nti_backend.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long teamId;
    private Long userId;
    private String role;
    private String inviteStatus;
    private String memberDisplayName;
    private String memberEmail;
    private String teamName;
}