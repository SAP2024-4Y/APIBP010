package com.hospital.appointment.graphql;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.BookAppointmentRequest;
import com.hospital.appointment.dto.RescheduleAppointmentRequest;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.service.DoctorService;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.service.PatientService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AppointmentGraphQlController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    public AppointmentGraphQlController(
            AppointmentService appointmentService,
            PatientService patientService,
            DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    @QueryMapping
    public List<AppointmentDto> appointments(
            @Argument String doctorId,
            @Argument OffsetDateTime startsAt,
            @Argument OffsetDateTime endsAt) {
        return appointmentService.findByDoctorAndWindow(UUID.fromString(doctorId), startsAt, endsAt);
    }

    @QueryMapping
    public PatientDto patient(@Argument String id) {
        return patientService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public Map<String, Object> patients(
            @Argument Integer first,
            @Argument String after,
            @Argument String status) {
        int limit = first != null ? first : 20;
        ApiResponse<List<PatientDto>> response = patientService.list(status, limit, after);

        List<Map<String, Object>> edges = response.data().stream()
                .map(p -> Map.<String, Object>of(
                        "node", p,
                        "cursor", CursorUtils.encode(p.id() + "|" + p.id())))
                .toList();

        String endCursor = response.next_cursor() != null ? response.next_cursor() : "";
        Map<String, Object> pageInfo = Map.of(
                "hasNextPage", response.has_more(),
                "endCursor", endCursor);

        return Map.of("edges", edges, "pageInfo", pageInfo);
    }

    @MutationMapping
    public AppointmentDto bookAppointment(
            @Argument String doctorId,
            @Argument String patientId,
            @Argument OffsetDateTime startsAt,
            @Argument OffsetDateTime endsAt,
            @Argument String type,
            @Argument String notes) {
        BookAppointmentRequest req = new BookAppointmentRequest(
                UUID.fromString(doctorId),
                UUID.fromString(patientId),
                startsAt,
                endsAt,
                type,
                notes);
        return appointmentService.book(req);
    }

    @MutationMapping
    public AppointmentDto cancelAppointment(@Argument String id) {
        return appointmentService.cancel(UUID.fromString(id));
    }

    @MutationMapping
    public AppointmentDto rescheduleAppointment(
            @Argument String id,
            @Argument OffsetDateTime startsAt,
            @Argument OffsetDateTime endsAt) {
        return appointmentService.reschedule(
                UUID.fromString(id),
                new RescheduleAppointmentRequest(startsAt, endsAt));
    }

    @SchemaMapping(typeName = "Appointment", field = "doctor")
    public DoctorDto appointmentDoctor(AppointmentDto appointment) {
        if (appointment.doctorId() == null) {
            return null;
        }
        return doctorService.findById(appointment.doctorId());
    }

    @SchemaMapping(typeName = "Appointment", field = "patient")
    public PatientDto appointmentPatient(AppointmentDto appointment) {
        if (appointment.patientId() == null) {
            return null;
        }
        return patientService.findById(appointment.patientId());
    }

    @SchemaMapping(typeName = "Patient", field = "appointments")
    public List<AppointmentDto> patientAppointments(PatientDto patient) {
        return appointmentService.findByPatient(patient.id());
    }
}
