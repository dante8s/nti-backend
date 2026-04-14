package com.nti.nti_backend.program;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;

    // Всі активні програми — публічний доступ
    public List<ProgramDTO> getAllActive() {
        return programRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Одна програма по id
    public ProgramDTO getById(Long id) {
        return programRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() ->
                        new RuntimeException("Програму не знайдено")
                );
    }

    // Створити нову програму (тільки ADMIN)
    public ProgramDTO create(ProgramDTO dto) {
        Program program = Program.builder()
                .name(dto.name())
                .description(dto.description())
                .type(ProgramType.valueOf(dto.type()))
                .build();
        return toDTO(programRepository.save(program));
    }

    // Оновити програму (тільки ADMIN)
    public ProgramDTO update(Long id, ProgramDTO dto) {
        Program program = programRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Програму не знайдено")
                );
        program.setName(dto.name());
        program.setDescription(dto.description());
        program.setActive(dto.isActive());
        return toDTO(programRepository.save(program));
    }

    // Деактивувати програму
    public void deactivate(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Програму не знайдено")
                );
        program.setActive(false);
        programRepository.save(program);
    }

    private ProgramDTO toDTO(Program p) {
        return new ProgramDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getType().name(),
                p.isActive()
        );
    }
}