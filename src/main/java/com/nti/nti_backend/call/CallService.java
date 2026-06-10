package com.nti.nti_backend.call;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.program.Program;
import com.nti.nti_backend.program.ProgramRepository;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.nti.nti_backend.config.CacheNames.*;
import org.springframework.cache.annotation.*;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final ProgramRepository programRepository;
    private final AuditService auditService;

    // All open calls for a program
    @Cacheable(value = CALLS_OPEN, key = "#programId")
    public List<CallDTO> getOpenByProgram(Long programId) {
        return callRepository
                .findByProgramIdAndStatus(
                        programId, CallStatus.OPEN
                )
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // All open calls overall
    @Cacheable(value = CALLS_OPEN, key = "'all'")
    public List<CallDTO> getAllOpen() {
        return callRepository
                .findByStatus(CallStatus.OPEN)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Single call by id
    @Cacheable(value = CALL, key = "#id")
    public CallDTO getById(Long id) {
        return callRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() ->
                        new RuntimeException("Call not found")
                );
    }

    // Create call (ADMIN)
    @Caching(evict = {
            @CacheEvict(value = CALLS_OPEN, allEntries = true),
            @CacheEvict(value = CALLS_BY_PROGRAM, key = "#programId")
    })

    public CallDTO create(Long programId, CreateCallRequest request, User actor) {
        Program program = programRepository
                .findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
        Call call = Call.builder()
                .title(request.title())
                .program(program)
                .deadline(request.deadline())
                .evaluationCriteria(request.evaluationCriteria())
                .build();
        CallDTO result = toDTO(callRepository.save(call));
        auditService.log(actor, "CALL_CREATED", "CALL", result.id(),
                "Call created: \"" + request.title() + "\" in program id=" + programId);
        return result;
    }

    // Close call (ADMIN)
    @Caching(evict = {
            @CacheEvict(value = CALLS_OPEN, allEntries = true),
            @CacheEvict(value = CALL, key = "#id")
    })
    public void close(Long id, User actor) {
        Call call = callRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Call not found"));
        call.setStatus(CallStatus.CLOSED);
        callRepository.save(call);
        auditService.log(actor, "CALL_CLOSED", "CALL", id,
                "Call closed: \"" + call.getTitle() + "\"");
    }

    // Get all calls for Program
    @Cacheable(
            value = CALLS_BY_PROGRAM,
            key = "#programId"
    )
    public List<CallDTO> getByProgram(Long programId) {
        return callRepository.findByProgramId(programId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
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