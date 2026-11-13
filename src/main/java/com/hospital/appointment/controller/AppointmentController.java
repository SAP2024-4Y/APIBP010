package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.BookAppointmentRequest;
import com.hospital.appointment.dto.RescheduleAppointmentRequest;
import com.hospital.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Appointment", description = "Appointment booking and management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/doctors/{doctorId}/appointments")
    @Operation(summary = "Calendar API: list appointments for a doctor in a time window")
    public ResponseEntity<List<AppointmentDto>> getByDoctorAndWindow(
            @PathVariable UUID doctorId,
            @RequestParam(name = "starts_at", required = false) OffsetDateTime startsAt,
            @RequestParam(name = "ends_at", required = false) OffsetDateTime endsAt) {
        return ResponseEntity.ok(appointmentService.findByDoctorAndWindow(doctorId, startsAt, endsAt));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Book an appointment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppointmentDto> book(@Valid @RequestBody BookAppointmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(req));
    }

    @PutMapping("/appointments/{id}")
    @Operation(summary = "Reschedule an appointment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppointmentDto> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest req) {
        return ResponseEntity.ok(appointmentService.reschedule(id, req));
    }

    @DeleteMapping("/appointments/{id}")
    @Operation(summary = "Cancel an appointment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppointmentDto> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancel(id));
    }
}
