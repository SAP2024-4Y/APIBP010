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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final TimeSlotRepository timeSlotRepository;

    public DoctorService(DoctorRepository doctorRepository, TimeSlotRepository timeSlotRepository) {
        this.doctorRepository = doctorRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public DoctorDto findById(UUID id) {
        return doctorRepository.findById(id)
                .map(DoctorDto::from)
                .orElseThrow(() -> new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found: " + id));
    }

    public Map<UUID, DoctorDto> findByIds(List<UUID> ids) {
        return doctorRepository.findByIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Doctor::getId, DoctorDto::from));
    }

    public ApiResponse<List<DoctorDto>> list(
            DoctorStatus status,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortField,
            String order,
            int limit,
            String afterCursor
    ) {
        String effectiveSort = (sortField != null && !sortField.isBlank()) ? sortField : "id";
        boolean asc = !"desc".equalsIgnoreCase(order);

        // Decode cursor: "<lastId>|<lastSortValue>"
        String lastId = null;
        String lastSortValue = null;
        if (afterCursor != null && !afterCursor.isBlank()) {
            String decoded = CursorUtils.decode(afterCursor);
            String[] parts = decoded.split("\\|", 2);
            lastId = parts[0];
            if (parts.length > 1) lastSortValue = parts[1];
        }

        final String fLastId = lastId;
        final String fLastSortValue = lastSortValue;

        Specification<Doctor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (category != null && !category.isBlank())
                predicates.add(cb.equal(root.get("specialization"), category));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("consultationFee"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("consultationFee"), maxPrice));

            // Keyset pagination
            if (fLastId != null && fLastSortValue != null && !"id".equals(effectiveSort)) {
                // e.g. (sortField > lastSortValue) OR (sortField = lastSortValue AND id > lastId)
                Predicate gt = asc
                        ? cb.greaterThan(root.get(effectiveSort).as(String.class), fLastSortValue)
                        : cb.lessThan(root.get(effectiveSort).as(String.class), fLastSortValue);
                Predicate eqAndGtId = cb.and(
                        cb.equal(root.get(effectiveSort).as(String.class), fLastSortValue),
                        asc ? cb.greaterThan(root.get("id").as(String.class), fLastId)
                            : cb.lessThan(root.get("id").as(String.class), fLastId)
                );
                predicates.add(cb.or(gt, eqAndGtId));
            } else if (fLastId != null) {
                predicates.add(asc
                        ? cb.greaterThan(root.get("id").as(String.class), fLastId)
                        : cb.lessThan(root.get("id").as(String.class), fLastId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, effectiveSort)
                .and(Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, "id"));

        // Fetch limit+1 to detect has_more
        List<Doctor> results = doctorRepository.findAll(spec, sort).stream()
                .limit(limit + 1L)
                .toList();

        boolean hasMore = results.size() > limit;
        List<Doctor> page = hasMore ? results.subList(0, limit) : results;

        String nextCursor = null;
        if (hasMore) {
            Doctor last = page.get(page.size() - 1);
            String sortVal = getSortValue(last, effectiveSort);
            nextCursor = CursorUtils.encode(last.getId() + "|" + sortVal);
        }

        return ApiResponse.of(page.stream().map(DoctorDto::from).toList(), hasMore, nextCursor);
    }

    private String getSortValue(Doctor d, String field) {
        return switch (field) {
            case "name" -> d.getName();
            case "specialization" -> d.getSpecialization();
            case "consultationFee", "consultation_fee" -> d.getConsultationFee().toPlainString();
            case "status" -> d.getStatus().name();
            default -> d.getId().toString();
        };
    }

    public HeatmapDto heatmap(UUID doctorId, String weekParam) {
        // Verify doctor exists
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found: " + doctorId));

        // Parse ISO week e.g. "2026-W48"
        String[] parts = weekParam.split("-W");
        if (parts.length != 2) {
            throw new DomainException(ErrorCode.INVALID_DATE_RANGE, "Invalid week format. Use YYYY-Www e.g. 2026-W48");
        }
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);

        LocalDate monday = LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<TimeSlot> slots = timeSlotRepository.findByDoctorIdAndSlotDateBetween(doctorId, monday, sunday);

        // Build 7x24 grid — all zeros initially
        String[] dayKeys = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        Map<String, int[]> grid = new LinkedHashMap<>();
        for (String key : dayKeys) grid.put(key, new int[24]);

        for (TimeSlot slot : slots) {
            if (!slot.isAvailable()) continue;
            DayOfWeek dow = slot.getSlotDate().getDayOfWeek();
            String key = dow.name().substring(0, 3);
            int hour = slot.getStartTime().getHour();
            grid.get(key)[hour] = 1;
        }

        return new HeatmapDto(doctorId, weekParam, grid);
    }
}
