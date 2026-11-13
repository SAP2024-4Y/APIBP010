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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final DepartmentService departmentService;

    public HospitalService(HospitalRepository hospitalRepository,
            DepartmentService departmentService) {
        this.hospitalRepository = hospitalRepository;
        this.departmentService = departmentService;
    }

    public HospitalDto getById(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.HOSPITAL_NOT_FOUND, "Hospital not found: " + id));
        List<DepartmentDto> departments = departmentService.findByHospitalId(id);
        return HospitalDto.from(hospital, departments);
    }

    public ApiResponse<List<HospitalDto>> list(
            HospitalStatus status, HospitalType category, int limit, String afterCursor) {

        String lastId = null;
        if (afterCursor != null && !afterCursor.isBlank()) {
            lastId = CursorUtils.decode(afterCursor).split("\\|", 2)[0];
        }

        final String fLastId = lastId;

        Specification<Hospital> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("hospitalType"), category));
            }
            if (fLastId != null) {
                predicates.add(cb.greaterThan(root.get("id").as(String.class), fLastId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<Hospital> results = hospitalRepository
                .findAll(spec, PageRequest.of(0, limit + 1, sort))
                .getContent();

        boolean hasMore = results.size() > limit;
        List<Hospital> page = hasMore ? results.subList(0, limit) : results;

        String nextCursor = null;
        if (hasMore) {
            Hospital last = page.get(page.size() - 1);
            nextCursor = CursorUtils.encode(last.getId() + "|" + last.getId());
        }

        return ApiResponse.of(
                page.stream().map(HospitalDto::from).toList(), hasMore, nextCursor);
    }

    public List<HospitalDto> findNearby(double latitude, double longitude, double radiusKm) {
        return hospitalRepository.findNearby(latitude, longitude, radiusKm * 1000)
                .stream().map(HospitalDto::from).toList();
    }

    public List<HospitalDto> findInBox(
            double minLat, double minLon, double maxLat, double maxLon) {
        return hospitalRepository.findInBox(minLat, minLon, maxLat, maxLon)
                .stream().map(HospitalDto::from).toList();
    }
}
