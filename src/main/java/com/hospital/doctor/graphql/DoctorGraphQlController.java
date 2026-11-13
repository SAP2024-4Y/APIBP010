package com.hospital.doctor.graphql;

import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.service.DepartmentService;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.HeatmapDto;
import com.hospital.doctor.entity.DoctorStatus;
import com.hospital.doctor.service.DoctorService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class DoctorGraphQlController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    public DoctorGraphQlController(DoctorService doctorService, DepartmentService departmentService) {
        this.doctorService = doctorService;
        this.departmentService = departmentService;
    }

    @QueryMapping
    public Map<String, Object> doctors(
            @Argument Integer first,
            @Argument String after,
            @Argument String status,
            @Argument String category,
            @Argument Double minPrice,
            @Argument Double maxPrice,
            @Argument String sort,
            @Argument String order
    ) {
        int limit = first != null ? first : 20;
        DoctorStatus doctorStatus = status != null ? DoctorStatus.valueOf(status.toUpperCase()) : null;
        BigDecimal min = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal max = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        ApiResponse<List<DoctorDto>> response =
                doctorService.list(doctorStatus, category, min, max, sort, order, limit, after);

        // Use the next_cursor from the response as the last edge cursor (correct keyset value)
        List<Map<String, Object>> edges = response.data().stream()
                .map(d -> {
                    String cursor = CursorUtils.encode(d.id().toString() + "|" + d.id().toString());
                    return Map.<String, Object>of("node", d, "cursor", cursor);
                })
                .toList();

        String endCursor = response.next_cursor() != null ? response.next_cursor() : "";
        Map<String, Object> pageInfo = Map.of(
                "hasNextPage", response.has_more(),
                "endCursor", endCursor
        );

        return Map.of("edges", edges, "pageInfo", pageInfo);
    }

    @QueryMapping
    public DoctorDto doctor(@Argument String id) {
        return doctorService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public List<DepartmentDto> departments() {
        return departmentService.findAll();
    }

    @QueryMapping
    public HeatmapDto doctorHeatmap(@Argument String doctorId, @Argument String week) {
        return doctorService.heatmap(UUID.fromString(doctorId), week);
    }

    @QueryMapping
    public List<DoctorDto> doctorsBySpecialty(@Argument String specialty) {
        return doctorService.list(null, specialty, null, null, null, "asc", 100, null).data();
    }

    @SchemaMapping(typeName = "Doctor", field = "department")
    public DepartmentDto department(DoctorDto doctor) {
        if (doctor.departmentId() == null) {
            return null;
        }
        return departmentService.findByIds(List.of(doctor.departmentId())).get(doctor.departmentId());
    }

    @SchemaMapping(typeName = "Doctor", field = "location")
    public Map<String, Double> location(DoctorDto doctor) {
        if (doctor.location() == null) {
            return null;
        }
        return Map.of("longitude", doctor.location()[0], "latitude", doctor.location()[1]);
    }

    @SchemaMapping(typeName = "DoctorHeatmap", field = "grid")
    public Map<String, List<Integer>> heatmapGrid(HeatmapDto heatmap) {
        return heatmap.grid();
    }
}
