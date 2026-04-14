package com.nti.nti_backend.call;

import com.nti.nti_backend.program.Program;
import com.nti.nti_backend.program.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final ProgramRepository programRepository;

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
    public CallDTO create(
            Long programId,
            CreateCallRequest request) {
        Program program = programRepository
                .findById(programId)
                .orElseThrow(() ->
                        new RuntimeException("Програму не знайдено")
                );
        Call call = Call.builder()
                .title(request.title())
                .program(program)
                .deadline(request.deadline())
                .evaluationCriteria(request.evaluationCriteria())
                .build();
        return toDTO(callRepository.save(call));
    }

    // Закрити виклик (ADMIN)
    public void close(Long id) {
        Call call = callRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Виклик не знайдено")
                );
        call.setStatus(CallStatus.CLOSED);
        callRepository.save(call);
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