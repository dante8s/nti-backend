package com.nti.nti_backend.team;

import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.teamMember.TeamMemberRepository;
import com.nti.nti_backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceInviteTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private com.nti.nti_backend.user.UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    private Team team;

    @BeforeEach
    void setUpTeam() {
        team = new Team();
        team.setId(99L);
        team.setMaxCapacity(3);
    }

    @Test
    @DisplayName("inviteMember rejects unknown user id")
    void invite_unknownUser_throws() {
        when(teamRepository.findById(99L)).thenReturn(Optional.of(team));
        when(userRepository.findById(255L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.inviteMember(99L, 255L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не існує");
    }

    @Test
    @DisplayName("inviteMember creates PENDING invite when eligible")
    void invite_success() {
        User guest = User.builder().id(5L).name("Guest").email("g@test").password("x").build();

        when(teamRepository.findById(99L)).thenReturn(Optional.of(team));
        when(userRepository.findById(5L)).thenReturn(Optional.of(guest));
        when(teamMemberRepository.existsByTeam_IdAndUser_Id(99L, 5L)).thenReturn(false);
        when(teamMemberRepository.existsByUser_IdAndInviteStatus(
                5L, TeamMember.InviteStatus.ACCEPTED)).thenReturn(false);
        when(teamMemberRepository.countAcceptedMembers(99L)).thenReturn(1L);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(i -> i.getArgument(0));

        TeamMember result = teamService.inviteMember(99L, 5L);

        assertThat(result.getInviteStatus()).isEqualTo(TeamMember.InviteStatus.PENDING);
        assertThat(result.getRole()).isEqualTo(TeamMember.TeamRole.MEMBER);
        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getTeam()).isSameAs(team);
    }
}
