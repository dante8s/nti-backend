package com.nti.nti_backend.call;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CallRepository
        extends JpaRepository<Call, Long> {

    List<Call> findByProgramId(Long programId);

    List<Call> findByProgramIdAndStatus(
            Long programId, CallStatus status
    );

    List<Call> findByStatus(CallStatus status);
}