package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.AddNoteRequestDTO;
import com.nti.nti_backend.mentorship.dto.ConsultationNoteDTO;
import com.nti.nti_backend.mentorship.entity.ConsultationNote;
import com.nti.nti_backend.mentorship.repository.ConsultationNoteRepository;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationNoteService {

    private final ConsultationNoteRepository consultationNoteRepository;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private ConsultationNoteDTO toDTO(ConsultationNote n) {
        return ConsultationNoteDTO.builder()
                .id(n.getId())
                .applicationId(n.getApplicationId())
                .content(n.getContent())
                .createdById(n.getCreatedBy().getId())
                .createdByName(n.getCreatedBy().getName())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }

    // CREATE
    @Transactional
    public ConsultationNoteDTO create(AddNoteRequestDTO dto) {
        User currentUser = getCurrentUser();

        if (!currentUser.getRole().name().equals("MENTOR")) {
            throw new ConflictException("Only MENTOR can write consultation notes");
        }

        ConsultationNote note = ConsultationNote.builder()
                .applicationId(dto.getApplicationId())
                .content(dto.getContent())
                .createdBy(currentUser)
                .build();

        return toDTO(consultationNoteRepository.save(note));
    }

    // GET all notes for an application
    @Transactional(readOnly = true)
    public List<ConsultationNoteDTO> getByApplication(Long applicationId) {
        return consultationNoteRepository
                .findAllByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // UPDATE author only
    @Transactional
    public ConsultationNoteDTO update(UUID noteId,AddNoteRequestDTO dto) {
        ConsultationNote note = consultationNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found: " + noteId
                ));

        User currentUser = getCurrentUser();
        if (!note.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ConflictException("Only the author can edit this note");
        }

        note.setContent(dto.getContent());
        return toDTO(consultationNoteRepository.save(note));
    }

    // DELETE - author only
    @Transactional
    public void delete(UUID noteId) {
        ConsultationNote note = consultationNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found: " + noteId
                ));

        User currentUser = getCurrentUser();
        if (!note.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ConflictException("Only the author can remove this note");
        }

        consultationNoteRepository.delete(note);
    }
}
