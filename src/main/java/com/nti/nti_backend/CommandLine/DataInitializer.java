package com.nti.nti_backend.CommandLine;

import com.nti.nti_backend.program.*;
import com.nti.nti_backend.call.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProgramRepository programRepository;
    private final CallRepository callRepository;

    @Override
    public void run(String... args) {
        // Створити тестові програми
        Program programA = Program.builder()
                .name("Program A - Innovation")
                .description("Програма для інноваційних проєктів")
                .type(ProgramType.PROGRAM_A)
                .isActive(true)
                .build();
        programA = programRepository.save(programA);

        Program programB = Program.builder()
                .name("Program B - Research")
                .description("Програма для дослідницьких проєктів")
                .type(ProgramType.PROGRAM_B)
                .isActive(true)
                .build();
        programB = programRepository.save(programB);

        // Створити тестові виклики
        Call call1 = Call.builder()
                .title("Виклик 1: Штучний інтелект")
                .program(programA)
                .deadline(LocalDateTime.now().plusMonths(3))
                .status(CallStatus.OPEN)
                .evaluationCriteria("Інноваційність, реалізованість, вплив")
                .build();
        callRepository.save(call1);

        Call call2 = Call.builder()
                .title("Виклик 2: Зелена енергетика")
                .program(programB)
                .deadline(LocalDateTime.now().plusMonths(2))
                .status(CallStatus.OPEN)
                .evaluationCriteria("Екологічність, ефективність")
                .build();
        callRepository.save(call2);

        System.out.println("✅ Тестові дані додано!");
    }
}
