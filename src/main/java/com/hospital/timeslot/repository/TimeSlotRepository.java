package com.hospital.timeslot.repository;

import com.hospital.timeslot.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    List<TimeSlot> findByDoctorIdAndSlotDateBetween(UUID doctorId, LocalDate startDate, LocalDate endDate);

    List<TimeSlot> findByDoctorIdAndSlotDate(UUID doctorId, LocalDate slotDate);
}
