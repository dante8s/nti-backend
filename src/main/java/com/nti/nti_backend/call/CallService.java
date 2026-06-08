package com.nti.nti_backend.call;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.program.Program;
import com.nti.nti_backend.program.ProgramRepository;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final ProgramRepository programRepository;
    private final AuditService auditService;

    // Всі відкриті виклики по програмі
    public List<CallDTO> getOpenByProgram(Long programId) {
        return callRepository
                .findByProgramIdAndStatus(
                        programId, CallStatus.OPEN
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Всі відкриті виклики взагалі
    public List<CallDTO> getAllOpen() {
        return callRepository
                .findByStatus(CallStatus.OPEN)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Один виклик по id
    public CallDTO getById(Long id) {
        return callRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() ->
                        new RuntimeException("Виклик не знайдено")
                );
    }

    // Створити виклик (ADMIN)
    public CallDTO create(Long programId, CreateCallRequest request, User actor) {
        Program program = programRepository
                .findById(programId)
                .orElseThrow(() -> new RuntimeException("Програму не знайдено"));
        Call call = Call.builder()
                .title(request.title())
                .program(program)
                .deadline(request.deadline())
                .evaluationCriteria(request.evaluationCriteria())
                .build();
        CallDTO result = toDTO(callRepository.save(call));
        auditService.log(actor, "CALL_CREATED", "CALL", result.id(),
                "Створено виклик: \"" + request.title() + "\" у програмі id=" + programId);
        return result;
    }

    // Закрити виклик (ADMIN)
    public void close(Long id, User actor) {
        Call call = callRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Виклик не знайдено"));
        call.setStatus(CallStatus.CLOSED);
        callRepository.save(call);
        auditService.log(actor, "CALL_CLOSED", "CALL", id,
                "Виклик закрито: \"" + call.getTitle() + "\"");
    }

    // Get all calls for Program
    public List<CallDTO> getByProgram(Long programId) {
        return callRepository.findByProgramId(programId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private CallDTO toDTO(Call c) {
        return new CallDTO(
                c.getId(),
                c.getTitle(),
                c.getProgram().getId(),
                c.getProgram().getName(),
                c.getDeadline(),
                c.getStatus().name(),
                c.getEvaluationCriteria()
        );
    }
}