package com.hospital.patient.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank String name,
        LocalDate dateOfBirth,
        String contact,
        String email
) {}
