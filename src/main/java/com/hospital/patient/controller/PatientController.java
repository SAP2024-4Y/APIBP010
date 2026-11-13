package com.hospital.patient.controller;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.common.ApiResponse;
import com.hospital.patient.dto.CreatePatientRequest;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "List patients with cursor pagination")
    public ResponseEntity<ApiResponse<List<PatientDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String after) {
        return ResponseEntity.ok(patientService.list(status, limit, after));
    }

    @PostMapping
    @Operation(summary = "Create a new patient", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PatientDto> create(@Valid @RequestBody CreatePatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID with appointment history")
    public ResponseEntity<PatientWithHistoryResponse> getById(@PathVariable UUID id) {
        PatientDto patient = patientService.findById(id);
        List<AppointmentDto> appointments = appointmentService.findByPatient(id);
        return ResponseEntity.ok(new PatientWithHistoryResponse(patient, appointments));
    }

    public record PatientWithHistoryResponse(PatientDto patient, List<AppointmentDto> appointments) {}
}
