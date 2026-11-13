package com.hospital.doctor.service;

import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.common.DomainException;
import com.hospital.common.ErrorCode;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.HeatmapDto;
import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.entity.DoctorStatus;
import com.hospital.doctor.repository.DoctorRepository;
import com.hospital.timeslot.entity.TimeSlot;
import com.hospital.timeslot.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    private DoctorService service;

    @BeforeEach
    void setUp() {
        service = new DoctorService(doctorRepository, timeSlotRepository);
    }

    private Doctor makeDoctor(String name) {
        Doctor d = new Doctor();
        d.setId(UUID.randomUUID());
        d.setName(name);
        d.setSpecialization("Cardiology");
        d.setConsultationFee(new BigDecimal("500.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        d.setDepartmentId(UUID.randomUUID());
        return d;
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findByIdReturnsDtoForExistingDoctor() {
        Doctor d = makeDoctor("Dr. Smith");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));

        DoctorDto dto = service.findById(d.getId());

        assertThat(dto.id()).isEqualTo(d.getId());
        assertThat(dto.name()).isEqualTo("Dr. Smith");
    }

    @Test
    void findByIdThrowsDoctorNotFoundForMissingDoctor() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode())
                        .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND));
    }

    // ── findByIds ─────────────────────────────────────────────────────────────

    @Test
    void findByIdsReturnsMapKeyedById() {
        Doctor d1 = makeDoctor("Dr. A");
        Doctor d2 = makeDoctor("Dr. B");
        when(doctorRepository.findByIdIn(List.of(d1.getId(), d2.getId())))
                .thenReturn(List.of(d1, d2));

        Map<UUID, DoctorDto> result = service.findByIds(List.of(d1.getId(), d2.getId()));

        assertThat(result).containsKeys(d1.getId(), d2.getId());
        assertThat(result.get(d1.getId()).name()).isEqualTo("Dr. A");
        assertThat(result.get(d2.getId()).name()).isEqualTo("Dr. B");
    }

    @Test
    void findByIdsReturnsEmptyMapForNoIds() {
        when(doctorRepository.findByIdIn(List.of())).thenReturn(List.of());
        assertThat(service.findByIds(List.of())).isEmpty();
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void listReturnsDtosForAllResultsWithinLimit() {
        Doctor d = makeDoctor("Dr. Page");
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, null, "asc", 20, null);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).name()).isEqualTo("Dr. Page");
    }

    @Test
    void listHasMoreIsTrueWhenRepoReturnsLimitPlusOne() {
        // Service fetches limit+1; receiving 3 when limit=2 means has_more=true
        List<Doctor> doctors = List.of(makeDoctor("A"), makeDoctor("B"), makeDoctor("C"));
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(doctors));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, null, "asc", 2, null);

        assertThat(response.has_more()).isTrue();
        assertThat(response.data()).hasSize(2);
    }

    @Test
    void listHasMoreIsFalseWhenResultsAtOrBelowLimit() {
        List<Doctor> doctors = List.of(makeDoctor("A"), makeDoctor("B"));
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(doctors));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, null, "asc", 2, null);

        assertThat(response.has_more()).isFalse();
        assertThat(response.next_cursor()).isNull();
    }

    @Test
    void listNextCursorIsBase64EncodedIdPipeSortValue() {
        List<Doctor> doctors = List.of(makeDoctor("A"), makeDoctor("B"), makeDoctor("C"));
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(doctors));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, null, "asc", 2, null);

        String decoded = CursorUtils.decode(response.next_cursor());
        assertThat(decoded).contains("|");
    }

    @Test
    void listReturnsEmptyPageWhenNoResults() {
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, null, "asc", 20, null);

        assertThat(response.data()).isEmpty();
        assertThat(response.has_more()).isFalse();
        assertThat(response.next_cursor()).isNull();
    }

    @Test
    void listWithAfterCursorDecodesAndPassesThrough() {
        Doctor d = makeDoctor("Dr. Z");
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        String cursor = CursorUtils.encode(UUID.randomUUID() + "|Dr. A");
        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, "name", "asc", 20, cursor);

        assertThat(response.data()).hasSize(1);
    }

    @Test
    void listDescOrderPassedThrough() {
        Doctor d = makeDoctor("Dr. Desc");
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        ApiResponse<List<DoctorDto>> response =
                service.list(null, null, null, null, "name", "desc", 20, null);

        assertThat(response.data()).hasSize(1);
    }

    // ── heatmap ───────────────────────────────────────────────────────────────

    @Test
    void heatmapThrowsDoctorNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.heatmap(id, "2026-W48"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode())
                        .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND));
    }

    @Test
    void heatmapThrowsInvalidDateRangeForMalformedWeek() {
        Doctor d = makeDoctor("Dr. Bad");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.heatmap(d.getId(), "2026W48"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode())
                        .isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void heatmapGridContainsAllSevenDayKeys() {
        Doctor d = makeDoctor("Dr. Grid");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));
        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        assertThat(result.grid()).containsKeys("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    }

    @Test
    void heatmapEachDayHas24HourSlots() {
        Doctor d = makeDoctor("Dr. Hours");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));
        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        result.grid().values().forEach(hours -> assertThat(hours).hasSize(24));
    }

    @Test
    void heatmapNoSlotsProducesAllZeros() {
        Doctor d = makeDoctor("Dr. Empty");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));
        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        result.grid().values()
                .forEach(hours -> hours.forEach(h -> assertThat(h).isEqualTo(0)));
    }

    @Test
    void heatmapAvailableSlotMarksHourAsOne() {
        Doctor d = makeDoctor("Dr. Avail");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));

        // 2026-W48 Monday = 2026-11-23
        TimeSlot slot = new TimeSlot();
        slot.setDoctorId(d.getId());
        slot.setSlotDate(LocalDate.of(2026, 11, 23));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setAvailable(true);

        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of(slot));

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        assertThat(result.grid().get("MON").get(9)).isEqualTo(1);
    }

    @Test
    void heatmapUnavailableSlotRemainsZero() {
        Doctor d = makeDoctor("Dr. Busy");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));

        TimeSlot slot = new TimeSlot();
        slot.setDoctorId(d.getId());
        slot.setSlotDate(LocalDate.of(2026, 11, 23));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setAvailable(false);

        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of(slot));

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        assertThat(result.grid().get("MON").get(10)).isEqualTo(0);
    }

    @Test
    void heatmapResultContainsCorrectDoctorIdAndWeek() {
        Doctor d = makeDoctor("Dr. Meta");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));
        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        assertThat(result.doctorId()).isEqualTo(d.getId());
        assertThat(result.week()).isEqualTo("2026-W48");
    }

    @Test
    void heatmapMultipleSlotsAcrossDays() {
        Doctor d = makeDoctor("Dr. Multi");
        when(doctorRepository.findById(d.getId())).thenReturn(Optional.of(d));

        // Monday slot at 9
        TimeSlot mon = new TimeSlot();
        mon.setSlotDate(LocalDate.of(2026, 11, 23)); // Monday W48
        mon.setStartTime(LocalTime.of(9, 0));
        mon.setEndTime(LocalTime.of(10, 0));
        mon.setAvailable(true);

        // Wednesday slot at 14
        TimeSlot wed = new TimeSlot();
        wed.setSlotDate(LocalDate.of(2026, 11, 25)); // Wednesday W48
        wed.setStartTime(LocalTime.of(14, 0));
        wed.setEndTime(LocalTime.of(15, 0));
        wed.setAvailable(true);

        when(timeSlotRepository.findByDoctorIdAndSlotDateBetween(any(), any(), any()))
                .thenReturn(List.of(mon, wed));

        HeatmapDto result = service.heatmap(d.getId(), "2026-W48");

        assertThat(result.grid().get("MON").get(9)).isEqualTo(1);
        assertThat(result.grid().get("WED").get(14)).isEqualTo(1);
        assertThat(result.grid().get("TUE").get(9)).isEqualTo(0);
    }
}
