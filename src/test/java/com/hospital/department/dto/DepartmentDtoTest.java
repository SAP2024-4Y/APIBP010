package com.hospital.department.dto;

import com.hospital.department.entity.Department;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DepartmentDtoTest {

    @Test
    void fromDepartmentMapsAllFields() {
        Department dept = new Department();
        dept.setId(UUID.randomUUID());
        dept.setName("Cardiology");
        dept.setCategory("Specialty");
        dept.setHospitalId(UUID.randomUUID());
        dept.setStatus("ACTIVE");

        DepartmentDto dto = DepartmentDto.from(dept);

        assertThat(dto.id()).isEqualTo(dept.getId());
        assertThat(dto.name()).isEqualTo("Cardiology");
        assertThat(dto.category()).isEqualTo("Specialty");
        assertThat(dto.hospitalId()).isEqualTo(dept.getHospitalId());
        assertThat(dto.status()).isEqualTo("ACTIVE");
    }

    @Test
    void fromDepartmentPreservesNullableHospitalId() {
        Department dept = new Department();
        dept.setId(UUID.randomUUID());
        dept.setName("General");
        dept.setCategory("General");
        dept.setHospitalId(null);
        dept.setStatus("ACTIVE");

        assertThat(DepartmentDto.from(dept).hospitalId()).isNull();
    }
}
