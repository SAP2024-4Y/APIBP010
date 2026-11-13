package com.hospital.hospital.dto;

import com.hospital.common.GeoJsonUtils;
import com.hospital.department.dto.DepartmentDto;
import com.hospital.hospital.entity.Hospital;

import java.util.List;
import java.util.UUID;

public record HospitalDto(
        UUID id,
        String name,
        String address,
        String type,
        String status,
        double[] location,
        List<DepartmentDto> departments
) {
    public static HospitalDto from(Hospital h) {
        double[] loc = h.getLocation() != null ? GeoJsonUtils.toGeoJson(h.getLocation()) : null;
        return new HospitalDto(
                h.getId(), h.getName(), h.getAddress(),
                h.getHospitalType().name(), h.getStatus().name(),
                loc, null);
    }

    public static HospitalDto from(Hospital h, List<DepartmentDto> departments) {
        double[] loc = h.getLocation() != null ? GeoJsonUtils.toGeoJson(h.getLocation()) : null;
        return new HospitalDto(
                h.getId(), h.getName(), h.getAddress(),
                h.getHospitalType().name(), h.getStatus().name(),
                loc, departments);
    }
}
