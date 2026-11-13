package com.hospital.appointment.dto;

import com.hospital.appointment.entity.Appointment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentDto(
        UUID id,
        UUID doctorId,
        UUID patientId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        String type,
        String notes
) {
    public static AppointmentDto from(Appointment a) {
        return new AppointmentDto(
                a.getId(),
                a.getDoctorId(),
                a.getPatientId(),
                a.getStartsAt(),
                a.getEndsAt(),
                a.getStatus(),
                a.getType(),
                a.getNotes()
        );
    }
}
