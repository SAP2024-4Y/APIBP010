package com.hospital.patient.service;

import com.hospital.common.ApiResponse;
import com.hospital.common.CursorUtils;
import com.hospital.common.DomainException;
import com.hospital.common.ErrorCode;
import com.hospital.patient.dto.CreatePatientRequest;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.repository.PatientRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public PatientDto findById(UUID id) {
        return patientRepository.findById(id)
                .map(PatientDto::from)
                .orElseThrow(() -> new DomainException(ErrorCode.PATIENT_NOT_FOUND, "Patient not found: " + id));
    }

    public Map<UUID, PatientDto> findByIds(List<UUID> ids) {
        return patientRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Patient::getId, PatientDto::from));
    }

    public ApiResponse<List<PatientDto>> list(String status, int limit, String afterCursor) {
        String lastId = null;
        if (afterCursor != null && !afterCursor.isBlank()) {
            String decoded = CursorUtils.decode(afterCursor);
            String[] parts = decoded.split("\\|", 2);
            lastId = parts[0];
        }

        final String fLastId = lastId;
        Specification<Patient> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fLastId != null) {
                predicates.add(cb.greaterThan(root.get("id").as(String.class), fLastId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<Patient> results = patientRepository.findAll(spec, PageRequest.of(0, limit + 1, sort)).getContent();

        boolean hasMore = results.size() > limit;
        List<Patient> page = hasMore ? results.subList(0, limit) : results;

        String nextCursor = null;
        if (hasMore) {
            Patient last = page.get(page.size() - 1);
            nextCursor = CursorUtils.encode(last.getId() + "|" + last.getId());
        }

        return ApiResponse.of(page.stream().map(PatientDto::from).toList(), hasMore, nextCursor);
    }

    @Transactional
    public PatientDto create(CreatePatientRequest req) {
        Patient patient = new Patient();
        patient.setName(req.name());
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setContact(req.contact());
        patient.setEmail(req.email());
        patient.setStatus("ACTIVE");
        return PatientDto.from(patientRepository.save(patient));
    }
}
