package com.hospital.timeslot.service;

import com.hospital.timeslot.dto.TimeSlotDto;
import com.hospital.timeslot.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<TimeSlotDto> findByDoctorAndDate(UUID doctorId, LocalDate date) {
        if (date != null) {
            return timeSlotRepository.findByDoctorIdAndSlotDate(doctorId, date).stream()
                    .map(TimeSlotDto::from)
                    .toList();
        }
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        return timeSlotRepository.findByDoctorIdAndSlotDateBetween(doctorId, today, endOfWeek).stream()
                .map(TimeSlotDto::from)
                .toList();
    }
}
