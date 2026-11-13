package com.hospital.doctor.dto;

import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.entity.DoctorStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DoctorDtoTest {

    private final GeometryFactory factory = new GeometryFactory();

    private Doctor baseDoctor() {
        Doctor d = new Doctor();
        d.setId(UUID.randomUUID());
        d.setName("Dr. Test");
        d.setSpecialization("Cardiology");
        d.setConsultationFee(new BigDecimal("500.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        d.setDepartmentId(UUID.randomUUID());
        return d;
    }

    @Test
    void fromDoctorMapsAllScalarFields() {
        Doctor d = baseDoctor();
        DoctorDto dto = DoctorDto.from(d);
        assertThat(dto.id()).isEqualTo(d.getId());
        assertThat(dto.name()).isEqualTo(d.getName());
        assertThat(dto.specialization()).isEqualTo(d.getSpecialization());
        assertThat(dto.consultationFee()).isEqualByComparingTo(d.getConsultationFee());
        assertThat(dto.status()).isEqualTo(d.getStatus().name());
        assertThat(dto.departmentId()).isEqualTo(d.getDepartmentId());
    }

    @Test
    void fromDoctorWithLocationReturnsGeoJsonOrder() {
        // GeoJSON convention: [longitude, latitude]
        Doctor d = baseDoctor();
        d.setLocation(factory.createPoint(new Coordinate(77.5946, 12.9716)));
        DoctorDto dto = DoctorDto.from(d);
        assertThat(dto.location()).isNotNull().hasSize(2);
        assertThat(dto.location()[0]).isEqualTo(77.5946); // longitude
        assertThat(dto.location()[1]).isEqualTo(12.9716); // latitude
    }

    @Test
    void fromDoctorWithNullLocationProducesNullLocation() {
        Doctor d = baseDoctor();
        d.setLocation(null);
        assertThat(DoctorDto.from(d).location()).isNull();
    }

    @Test
    void fromDoctorStatusIsMappedToString() {
        Doctor d = baseDoctor();
        d.setStatus(DoctorStatus.ON_LEAVE);
        assertThat(DoctorDto.from(d).status()).isEqualTo("ON_LEAVE");
    }
}
