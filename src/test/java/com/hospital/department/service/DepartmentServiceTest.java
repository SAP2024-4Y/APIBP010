package com.hospital.department.service;

import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.entity.Department;
import com.hospital.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departmentRepository);
    }

    private Department makeDept(String name) {
        Department dept = new Department();
        dept.setId(UUID.randomUUID());
        dept.setName(name);
        dept.setCategory("Specialty");
        dept.setHospitalId(UUID.randomUUID());
        dept.setStatus("ACTIVE");
        return dept;
    }

    @Test
    void findAllReturnsMappedDtos() {
        Department d1 = makeDept("Cardiology");
        Department d2 = makeDept("Neurology");
        when(departmentRepository.findAll()).thenReturn(List.of(d1, d2));

        List<DepartmentDto> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DepartmentDto::name)
                .containsExactly("Cardiology", "Neurology");
    }

    @Test
    void findAllReturnsEmptyListWhenNoDepartments() {
        when(departmentRepository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findByIdsReturnsMapKeyedById() {
        Department d1 = makeDept("Cardiology");
        Department d2 = makeDept("Neurology");
        List<UUID> ids = List.of(d1.getId(), d2.getId());
        when(departmentRepository.findByIdIn(ids)).thenReturn(List.of(d1, d2));

        Map<UUID, DepartmentDto> result = service.findByIds(ids);

        assertThat(result).containsKeys(d1.getId(), d2.getId());
        assertThat(result.get(d1.getId()).name()).isEqualTo("Cardiology");
        assertThat(result.get(d2.getId()).name()).isEqualTo("Neurology");
    }

    @Test
    void findByIdsReturnsEmptyMapForEmptyInput() {
        when(departmentRepository.findByIdIn(List.of())).thenReturn(List.of());
        assertThat(service.findByIds(List.of())).isEmpty();
    }

    @Test
    void findAllPreservesCategory() {
        Department dept = makeDept("Cardiology");
        dept.setCategory("Cardiac");
        when(departmentRepository.findAll()).thenReturn(List.of(dept));

        assertThat(service.findAll().get(0).category()).isEqualTo("Cardiac");
    }
}
