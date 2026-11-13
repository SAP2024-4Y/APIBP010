package com.hospital.timeslot.dto;

import com.hospital.timeslot.entity.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotDto(
        UUID id,
        UUID doctorId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean isAvailable
) {
    public static TimeSlotDto from(TimeSlot ts) {
        return new TimeSlotDto(
                ts.getId(),
                ts.getDoctorId(),
                ts.getSlotDate(),
                ts.getStartTime(),
                ts.getEndTime(),
                ts.isAvailable()
        );
    }
}
