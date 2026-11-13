package com.hospital.patient.dto;

import com.hospital.patient.entity.Patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientDto(
        UUID id,
        String name,
        LocalDate dateOfBirth,
        String contact,
        String email,
        String status
) {
    public static PatientDto from(Patient p) {
        return new PatientDto(
                p.getId(),
                p.getName(),
                p.getDateOfBirth(),
                p.getContact(),
                p.getEmail(),
                p.getStatus()
        );
    }
}
