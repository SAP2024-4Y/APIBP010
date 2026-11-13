package com.hospital.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID doctorId,
        @NotNull UUID patientId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        String type,
        String notes
) {}
