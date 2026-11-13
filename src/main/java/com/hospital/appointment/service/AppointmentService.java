package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.BookAppointmentRequest;
import com.hospital.appointment.dto.RescheduleAppointmentRequest;
import com.hospital.appointment.dto.SuggestionDto;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.common.DomainException;
import com.hospital.common.ErrorCode;
import com.hospital.doctor.repository.DoctorRepository;
import com.hospital.patient.repository.PatientRepository;
import com.hospital.timeslot.entity.TimeSlot;
import com.hospital.timeslot.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            TimeSlotRepository timeSlotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<AppointmentDto> findByDoctorAndWindow(UUID doctorId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new DomainException(ErrorCode.INVALID_DATE_RANGE, "starts_at must be before ends_at");
        }
        OffsetDateTime from = startsAt != null ? startsAt : OffsetDateTime.now(ZoneOffset.UTC).minusDays(365);
        OffsetDateTime to = endsAt != null ? endsAt : OffsetDateTime.now(ZoneOffset.UTC).plusDays(365);
        return appointmentRepository.findByDoctorAndWindow(doctorId, from, to).stream()
                .map(AppointmentDto::from)
                .toList();
    }

    public List<AppointmentDto> findByPatient(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
                .map(AppointmentDto::from)
                .toList();
    }

    @Transactional
    public AppointmentDto book(BookAppointmentRequest req) {
        validate(req.startsAt(), req.endsAt());
        checkDoctorExists(req.doctorId());
        checkPatientExists(req.patientId());
        checkWorkingHours(req.startsAt());
        checkOverlap(req.doctorId(), req.startsAt(), req.endsAt(), null);
        checkBuffer(req.doctorId(), req.startsAt(), req.endsAt(), null);

        Appointment appt = new Appointment();
        appt.setDoctorId(req.doctorId());
        appt.setPatientId(req.patientId());
        appt.setStartsAt(req.startsAt());
        appt.setEndsAt(req.endsAt());
        appt.setStatus("CONFIRMED");
        appt.setType(req.type() != null ? req.type() : "CONSULTATION");
        appt.setNotes(req.notes());
        return AppointmentDto.from(appointmentRepository.save(appt));
    }

    @Transactional
    public AppointmentDto reschedule(UUID id, RescheduleAppointmentRequest req) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PATIENT_NOT_FOUND, "Appointment not found: " + id));

        validate(req.startsAt(), req.endsAt());
        checkWorkingHours(req.startsAt());
        checkOverlap(appt.getDoctorId(), req.startsAt(), req.endsAt(), id);
        checkBuffer(appt.getDoctorId(), req.startsAt(), req.endsAt(), id);

        appt.setStartsAt(req.startsAt());
        appt.setEndsAt(req.endsAt());
        return AppointmentDto.from(appointmentRepository.save(appt));
    }

    @Transactional
    public AppointmentDto cancel(UUID id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PATIENT_NOT_FOUND, "Appointment not found: " + id));
        appt.setStatus("CANCELLED");
        return AppointmentDto.from(appointmentRepository.save(appt));
    }

    // ── Private validation helpers ────────────────────────────────────────────

    private void validate(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new DomainException(ErrorCode.INVALID_DATE_RANGE, "starts_at must be before ends_at");
        }
    }

    private void checkDoctorExists(UUID doctorId) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found: " + doctorId));
    }

    private void checkPatientExists(UUID patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new DomainException(ErrorCode.PATIENT_NOT_FOUND, "Patient not found: " + patientId));
    }

    private void checkWorkingHours(OffsetDateTime startsAt) {
        LocalTime t = startsAt.toLocalTime();
        if (t.isBefore(WORK_START) || t.isAfter(WORK_END) || t.equals(WORK_END)) {
            throw new DomainException(ErrorCode.OUTSIDE_WORKING_HOURS,
                    "Appointments can only be booked between 09:00 and 17:00");
        }
    }

    private void checkBuffer(UUID doctorId, OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeId) {
        OffsetDateTime bufferStart = startsAt.minusMinutes(15);
        OffsetDateTime bufferEnd = startsAt.plusMinutes(15);

        List<Appointment> nearby = excludeId == null
                ? appointmentRepository.findWithinBuffer(doctorId, bufferStart, bufferEnd)
                : appointmentRepository.findWithinBufferExcluding(doctorId, bufferStart, bufferEnd, excludeId);

        if (!nearby.isEmpty()) {
            throw new DomainException(ErrorCode.MINIMUM_BUFFER_VIOLATION,
                    "A minimum 15-minute buffer is required between appointments");
        }
    }

    private void checkOverlap(UUID doctorId, OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeId) {
        List<Appointment> overlapping = excludeId == null
                ? appointmentRepository.findOverlapping(doctorId, startsAt, endsAt)
                : appointmentRepository.findOverlappingExcluding(doctorId, startsAt, endsAt, excludeId);

        if (!overlapping.isEmpty()) {
            List<SuggestionDto> suggestions = findNextAvailableSlots(doctorId, endsAt, 3);
            throw new DomainException(ErrorCode.APPOINTMENT_CONFLICT,
                    "This slot is already booked.", suggestions);
        }
    }

    private List<SuggestionDto> findNextAvailableSlots(UUID doctorId, OffsetDateTime from, int count) {
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = startDate.plusDays(48);

        List<TimeSlot> candidates = timeSlotRepository
                .findByDoctorIdAndSlotDateBetween(doctorId, startDate, endDate);

        List<SuggestionDto> suggestions = new ArrayList<>();
        for (TimeSlot slot : candidates) {
            if (suggestions.size() >= count) {
                break;
            }
            if (!slot.isAvailable()) {
                continue;
            }

            OffsetDateTime slotStart = slot.getSlotDate()
                    .atTime(slot.getStartTime())
                    .atOffset(ZoneOffset.UTC);
            OffsetDateTime slotEnd = slot.getSlotDate()
                    .atTime(slot.getEndTime())
                    .atOffset(ZoneOffset.UTC);

            if (!slotStart.isAfter(from)) {
                continue;
            }

            List<Appointment> conflicts = appointmentRepository.findOverlapping(doctorId, slotStart, slotEnd);
            if (conflicts.isEmpty()) {
                suggestions.add(new SuggestionDto(slotStart, slotEnd));
            }
        }
        return suggestions;
    }
}
