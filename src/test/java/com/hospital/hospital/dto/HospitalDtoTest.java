package com.hospital.hospital.dto;

import com.hospital.department.dto.DepartmentDto;
import com.hospital.hospital.entity.Hospital;
import com.hospital.hospital.entity.HospitalStatus;
import com.hospital.hospital.entity.HospitalType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class HospitalDtoTest {

    private final GeometryFactory factory = new GeometryFactory();

    private Hospital baseHospital() {
        Hospital h = new Hospital();
        h.setId(UUID.randomUUID());
        h.setName("Test Hospital");
        h.setAddress("123 Test Street");
        h.setHospitalType(HospitalType.GENERAL);
        h.setStatus(HospitalStatus.ACTIVE);
        return h;
    }

    @Test
    void fromHospitalMapsAllScalarFields() {
        Hospital h = baseHospital();
        HospitalDto dto = HospitalDto.from(h);
        assertThat(dto.id()).isEqualTo(h.getId());
        assertThat(dto.name()).isEqualTo(h.getName());
        assertThat(dto.address()).isEqualTo(h.getAddress());
        assertThat(dto.type()).isEqualTo("GENERAL");
        assertThat(dto.status()).isEqualTo("ACTIVE");
    }

    @Test
    void fromHospitalWithLocationReturnsGeoJsonOrder() {
        // GeoJSON convention: [longitude, latitude]
        Hospital h = baseHospital();
        h.setLocation(factory.createPoint(new Coordinate(77.2090, 28.6139)));
        HospitalDto dto = HospitalDto.from(h);
        assertThat(dto.location()).isNotNull().hasSize(2);
        assertThat(dto.location()[0]).isEqualTo(77.2090); // longitude
        assertThat(dto.location()[1]).isEqualTo(28.6139); // latitude
    }

    @Test
    void fromHospitalWithNullLocationProducesNullLocation() {
        Hospital h = baseHospital();
        h.setLocation(null);
        assertThat(HospitalDto.from(h).location()).isNull();
    }

    @Test
    void fromHospitalSingleArgLeavesDepartmentsNull() {
        HospitalDto dto = HospitalDto.from(baseHospital());
        assertThat(dto.departments()).isNull();
    }

    @Test
    void fromHospitalWithDepartmentsIncludesDepartmentList() {
        Hospital h = baseHospital();
        DepartmentDto dept = new DepartmentDto(UUID.randomUUID(), "Cardiology", "MEDICAL", h.getId(), "ACTIVE");
        HospitalDto dto = HospitalDto.from(h, List.of(dept));
        assertThat(dto.departments()).hasSize(1);
        assertThat(dto.departments().get(0).name()).isEqualTo("Cardiology");
    }

    @Test
    void fromHospitalWithEmptyDepartmentsListIsEmpty() {
        HospitalDto dto = HospitalDto.from(baseHospital(), List.of());
        assertThat(dto.departments()).isEmpty();
    }

    @Test
    void fromHospitalStatusIsMappedToString() {
        Hospital h = baseHospital();
        h.setStatus(HospitalStatus.INACTIVE);
        assertThat(HospitalDto.from(h).status()).isEqualTo("INACTIVE");
    }

    @Test
    void fromHospitalTypeIsMappedToString() {
        Hospital h = baseHospital();
        h.setHospitalType(HospitalType.SPECIALTY);
        assertThat(HospitalDto.from(h).type()).isEqualTo("SPECIALTY");
    }
}
