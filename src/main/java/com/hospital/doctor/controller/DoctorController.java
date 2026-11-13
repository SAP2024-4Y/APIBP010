package com.hospital.doctor.controller;

import com.hospital.common.ApiResponse;
import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.service.DepartmentService;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.HeatmapDto;
import com.hospital.doctor.entity.DoctorStatus;
import com.hospital.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Doctor", description = "Doctor and Department endpoints")
public class DoctorController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    public DoctorController(DoctorService doctorService, DepartmentService departmentService) {
        this.doctorService = doctorService;
        this.departmentService = departmentService;
    }

    @GetMapping("/departments")
    @Operation(summary = "List all departments")
    public ResponseEntity<List<DepartmentDto>> getDepartments() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @GetMapping("/doctors")
    @Operation(summary = "List doctors with filtering, sorting and cursor pagination")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getDoctors(
            @RequestParam(required = false) DoctorStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "min_price") BigDecimal minPrice,
            @RequestParam(required = false, name = "max_price") BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) String after
    ) {
        return ResponseEntity.ok(
                doctorService.list(status, category, minPrice, maxPrice, sort, order, limit, after));
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<DoctorDto> getDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.findById(id));
    }

    @GetMapping("/doctors/{id}/heatmap")
    @Operation(summary = "Get doctor availability heatmap for an ISO week",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<HeatmapDto> getHeatmap(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "") String week
    ) {
        return ResponseEntity.ok(doctorService.heatmap(id, week));
    }
}
