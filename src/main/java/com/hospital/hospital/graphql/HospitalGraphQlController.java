package com.hospital.hospital.graphql;

import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.hospital.dto.HospitalDto;
import com.hospital.hospital.entity.HospitalStatus;
import com.hospital.hospital.entity.HospitalType;
import com.hospital.hospital.service.HospitalService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class HospitalGraphQlController {

    private final HospitalService hospitalService;

    public HospitalGraphQlController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @QueryMapping
    public Map<String, Object> hospitals(
            @Argument Integer first,
            @Argument String after,
            @Argument String status,
            @Argument String category) {
        int limit = first != null ? first : 20;
        HospitalStatus statusEnum = status != null
                ? HospitalStatus.valueOf(status.toUpperCase()) : null;
        HospitalType typeEnum = category != null
                ? HospitalType.valueOf(category.toUpperCase()) : null;

        ApiResponse<List<HospitalDto>> response =
                hospitalService.list(statusEnum, typeEnum, limit, after);

        List<Map<String, Object>> edges = response.data().stream()
                .map(dto -> {
                    String cursor = CursorUtils.encode(
                            dto.id().toString() + "|" + dto.id().toString());
                    return Map.<String, Object>of("node", dto, "cursor", cursor);
                })
                .toList();

        String endCursor = response.next_cursor() != null ? response.next_cursor() : "";
        Map<String, Object> pageInfo = Map.of(
                "hasNextPage", response.has_more(),
                "endCursor", endCursor);

        return Map.of("edges", edges, "pageInfo", pageInfo);
    }

    @QueryMapping
    public HospitalDto hospital(@Argument String id) {
        return hospitalService.getById(UUID.fromString(id));
    }

    @QueryMapping
    public List<HospitalDto> hospitalsNearby(
            @Argument double latitude,
            @Argument double longitude,
            @Argument double radiusKm) {
        return hospitalService.findNearby(latitude, longitude, radiusKm);
    }

    @QueryMapping
    public List<HospitalDto> hospitalsInBox(
            @Argument double minLat,
            @Argument double minLon,
            @Argument double maxLat,
            @Argument double maxLon) {
        return hospitalService.findInBox(minLat, minLon, maxLat, maxLon);
    }

    @SchemaMapping(typeName = "Hospital", field = "location")
    public Map<String, Double> location(HospitalDto hospital) {
        if (hospital.location() == null) {
            return null;
        }
        return Map.of("longitude", hospital.location()[0], "latitude", hospital.location()[1]);
    }
}
