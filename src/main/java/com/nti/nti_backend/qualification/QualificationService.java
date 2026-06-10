package com.nti.nti_backend.qualification;

import com.nti.nti_backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QualificationService {

    private final QualificationStackRepository stackRepo;
    private final QualificationSubjectRepository subjectRepo;

    public List<QualificationDTO.StackDTO> getAllStacks() {
        return stackRepo.findAllByOrderByStackNumberAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public QualificationDTO.StackDTO getByKey(String key) {
        return stackRepo.findBySpecializationKey(key)
                .map(this::toDTO)
                .orElseThrow(() -> AppException.notFound("Stack not found: " + key));
    }

    @Transactional
    public QualificationDTO.StackDTO addSubject(Long stackId, String subjectName) {
        QualificationStack stack = stackRepo.findById(stackId)
                .orElseThrow(() -> AppException.notFound("Stack not found"));

        int nextPos = stack.getSubjects().stream()
                .mapToInt(QualificationSubject::getPosition)
                .max()
                .orElse(0) + 1;

        QualificationSubject subject = QualificationSubject.builder()
                .stack(stack)
                .subjectName(subjectName.trim())
                .position(nextPos)
                .build();

        stack.getSubjects().add(subject);
        return toDTO(stackRepo.save(stack));
    }

    @Transactional
    public QualificationDTO.StackDTO removeSubject(Long stackId, Long subjectId) {
        QualificationStack stack = stackRepo.findById(stackId)
                .orElseThrow(() -> AppException.notFound("Stack not found"));

        boolean removed = stack.getSubjects().removeIf(s -> s.getId().equals(subjectId));
        if (!removed) throw AppException.notFound("Subject not found");

        return toDTO(stackRepo.save(stack));
    }

    private QualificationDTO.StackDTO toDTO(QualificationStack s) {
        List<QualificationDTO.SubjectDTO> subjects = s.getSubjects().stream()
                .map(sub -> new QualificationDTO.SubjectDTO(sub.getId(), sub.getSubjectName(), sub.getPosition()))
                .toList();
        return new QualificationDTO.StackDTO(
                s.getId(), s.getStackNumber(),
                s.getSpecializationKey(), s.getSpecializationName(),
                subjects
        );
    }
}
