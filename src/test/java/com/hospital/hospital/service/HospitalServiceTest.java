package com.hospital.hospital.service;

import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.common.DomainException;
import com.hospital.common.ErrorCode;
import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.service.DepartmentService;
import com.hospital.hospital.dto.HospitalDto;
import com.hospital.hospital.entity.Hospital;
import com.hospital.hospital.entity.HospitalStatus;
import com.hospital.hospital.entity.HospitalType;
import com.hospital.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DepartmentService departmentService;

    private HospitalService service;

    @BeforeEach
    void setUp() {
        service = new HospitalService(hospitalRepository, departmentService);
    }

    private Hospital makeHospital(String name) {
        Hospital h = new Hospital();
        h.setId(UUID.randomUUID());
        h.setName(name);
        h.setAddress("123 Test Street");
        h.setHospitalType(HospitalType.GENERAL);
        h.setStatus(HospitalStatus.ACTIVE);
        return h;
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getByIdReturnsDtoWithDepartmentsForExistingHospital() {
        Hospital h = makeHospital("AIIMS Delhi");
        DepartmentDto dept = new DepartmentDto(UUID.randomUUID(), "Cardiology", "MEDICAL", h.getId(), "ACTIVE");
        when(hospitalRepository.findById(h.getId())).thenReturn(Optional.of(h));
        when(departmentService.findByHospitalId(h.getId())).thenReturn(List.of(dept));

        HospitalDto dto = service.getById(h.getId());

        assertThat(dto.id()).isEqualTo(h.getId());
        assertThat(dto.name()).isEqualTo("AIIMS Delhi");
        assertThat(dto.departments()).hasSize(1);
        assertThat(dto.departments().get(0).name()).isEqualTo("Cardiology");
    }

    @Test
    void getByIdThrowsHospitalNotFoundForMissingHospital() {
        UUID id = UUID.randomUUID();
        when(hospitalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode())
                        .isEqualTo(ErrorCode.HOSPITAL_NOT_FOUND));
    }

    @Test
    void getByIdIncludesEmptyDepartmentListWhenNoneExist() {
        Hospital h = makeHospital("Empty Hospital");
        when(hospitalRepository.findById(h.getId())).thenReturn(Optional.of(h));
        when(departmentService.findByHospitalId(h.getId())).thenReturn(List.of());

        HospitalDto dto = service.getById(h.getId());

        assertThat(dto.departments()).isEmpty();
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void listReturnsDtosForAllResultsWithinLimit() {
        Hospital h = makeHospital("Apollo");
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));

        ApiResponse<List<HospitalDto>> response = service.list(null, null, 20, null);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).name()).isEqualTo("Apollo");
    }

    @Test
    void listHasMoreIsTrueWhenRepoReturnsLimitPlusOne() {
        List<Hospital> hospitals = List.of(
                makeHospital("A"), makeHospital("B"), makeHospital("C"));
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(hospitals));

        ApiResponse<List<HospitalDto>> response = service.list(null, null, 2, null);

        assertThat(response.has_more()).isTrue();
        assertThat(response.data()).hasSize(2);
    }

    @Test
    void listHasMoreIsFalseWhenResultsAtOrBelowLimit() {
        List<Hospital> hospitals = List.of(makeHospital("A"), makeHospital("B"));
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(hospitals));

        ApiResponse<List<HospitalDto>> response = service.list(null, null, 2, null);

        assertThat(response.has_more()).isFalse();
        assertThat(response.next_cursor()).isNull();
    }

    @Test
    void listNextCursorIsBase64EncodedIdPipeId() {
        List<Hospital> hospitals = List.of(
                makeHospital("A"), makeHospital("B"), makeHospital("C"));
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(hospitals));

        ApiResponse<List<HospitalDto>> response = service.list(null, null, 2, null);

        String decoded = CursorUtils.decode(response.next_cursor());
        assertThat(decoded).contains("|");
    }

    @Test
    void listReturnsEmptyPageWhenNoResults() {
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ApiResponse<List<HospitalDto>> response = service.list(null, null, 20, null);

        assertThat(response.data()).isEmpty();
        assertThat(response.has_more()).isFalse();
        assertThat(response.next_cursor()).isNull();
    }

    @Test
    void listWithAfterCursorDecodesAndPassesThrough() {
        Hospital h = makeHospital("Cursor Hospital");
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));

        String cursor = CursorUtils.encode(UUID.randomUUID() + "|" + UUID.randomUUID());
        ApiResponse<List<HospitalDto>> response = service.list(null, null, 20, cursor);

        assertThat(response.data()).hasSize(1);
    }

    @Test
    void listFiltersArePassedToSpecificationWithoutError() {
        Hospital h = makeHospital("Specialty Active");
        h.setHospitalType(HospitalType.SPECIALTY);
        when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));

        ApiResponse<List<HospitalDto>> response =
                service.list(HospitalStatus.ACTIVE, HospitalType.SPECIALTY, 20, null);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).type()).isEqualTo("SPECIALTY");
    }

    // ── findNearby ────────────────────────────────────────────────────────────

    @Test
    void findNearbyDelegatesToRepositoryWithMetreConvertedRadius() {
        Hospital h = makeHospital("Nearby Hospital");
        // repo is called with km * 1000; we just verify the delegation works
        when(hospitalRepository.findNearby(28.6139, 77.2090, 5000.0))
                .thenReturn(List.of(h));

        List<HospitalDto> results = service.findNearby(28.6139, 77.2090, 5.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Nearby Hospital");
    }

    @Test
    void findNearbyReturnsEmptyListWhenNoneWithinRadius() {
        when(hospitalRepository.findNearby(0.0, 0.0, 1000.0)).thenReturn(List.of());

        List<HospitalDto> results = service.findNearby(0.0, 0.0, 1.0);

        assertThat(results).isEmpty();
    }

    // ── findInBox ─────────────────────────────────────────────────────────────

    @Test
    void findInBoxDelegatesToRepositoryWithCorrectBounds() {
        Hospital h = makeHospital("Box Hospital");
        when(hospitalRepository.findInBox(28.0, 77.0, 29.0, 78.0))
                .thenReturn(List.of(h));

        List<HospitalDto> results = service.findInBox(28.0, 77.0, 29.0, 78.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Box Hospital");
    }

    @Test
    void findInBoxReturnsEmptyListWhenNoneInBounds() {
        when(hospitalRepository.findInBox(0.0, 0.0, 1.0, 1.0)).thenReturn(List.of());

        List<HospitalDto> results = service.findInBox(0.0, 0.0, 1.0, 1.0);

        assertThat(results).isEmpty();
    }
}
