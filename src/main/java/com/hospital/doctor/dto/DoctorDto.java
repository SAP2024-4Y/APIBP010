package com.hospital.doctor.dto;

import com.hospital.common.GeoJsonUtils;
import com.hospital.doctor.entity.Doctor;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorDto(
        UUID id,
        String name,
        String specialization,
        BigDecimal consultationFee,
        String status,
        UUID departmentId,
        double[] location
) {
    public static DoctorDto from(Doctor d) {
        double[] loc = d.getLocation() != null ? GeoJsonUtils.toGeoJson(d.getLocation()) : null;
        return new DoctorDto(
                d.getId(),
                d.getName(),
                d.getSpecialization(),
                d.getConsultationFee(),
                d.getStatus().name(),
                d.getDepartmentId(),
                loc
        );
    }
}
