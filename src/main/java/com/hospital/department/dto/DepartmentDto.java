package com.hospital.department.dto;

import com.hospital.department.entity.Department;

import java.util.UUID;

public record DepartmentDto(
        UUID id,
        String name,
        String category,
        UUID hospitalId,
        String status
) {
    public static DepartmentDto from(Department d) {
        return new DepartmentDto(d.getId(), d.getName(), d.getCategory(), d.getHospitalId(), d.getStatus());
    }
}
