package com.hospital.department.service;

import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.entity.Department;
import com.hospital.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(DepartmentDto::from)
                .toList();
    }

    public Map<UUID, DepartmentDto> findByIds(List<UUID> ids) {
        return departmentRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Department::getId, DepartmentDto::from));
    }

    public List<DepartmentDto> findByHospitalId(UUID hospitalId) {
        return departmentRepository.findByHospitalId(hospitalId).stream()
                .map(DepartmentDto::from)
                .toList();
    }
}
