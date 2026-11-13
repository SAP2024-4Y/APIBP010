package com.hospital.hospital.controller;

import com.hospital.common.ApiResponse;
import com.hospital.hospital.dto.HospitalDto;
import com.hospital.hospital.entity.HospitalStatus;
import com.hospital.hospital.entity.HospitalType;
import com.hospital.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@Tag(name = "Hospital", description = "Hospital management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    @Operation(summary = "List hospitals with optional filter and cursor pagination")
    public ResponseEntity<ApiResponse<List<HospitalDto>>> list(
            @RequestParam(required = false) HospitalStatus status,
            @RequestParam(required = false) HospitalType category,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String after) {
        return ResponseEntity.ok(hospitalService.list(status, category, limit, after));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find hospitals within a radius (km) of a coordinate")
    public ResponseEntity<List<HospitalDto>> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam("radius_km") double radiusKm) {
        return ResponseEntity.ok(hospitalService.findNearby(latitude, longitude, radiusKm));
    }

    @GetMapping("/search")
    @Operation(summary = "Find hospitals within a bounding box")
    public ResponseEntity<List<HospitalDto>> search(
            @RequestParam("min_lat") double minLat,
            @RequestParam("min_lon") double minLon,
            @RequestParam("max_lat") double maxLat,
            @RequestParam("max_lon") double maxLon) {
        return ResponseEntity.ok(hospitalService.findInBox(minLat, minLon, maxLat, maxLon));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a hospital by ID with its departments")
    public ResponseEntity<HospitalDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getById(id));
    }
}
