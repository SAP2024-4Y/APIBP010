package com.hospital.doctor.graphql;

import com.hospital.department.dto.DepartmentDto;
import com.hospital.department.service.DepartmentService;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DepartmentDataLoader {

    public DepartmentDataLoader(BatchLoaderRegistry registry, DepartmentService departmentService) {
        registry.<UUID, DepartmentDto>forTypePair(UUID.class, DepartmentDto.class)
                .withName("departmentLoader")
                .registerMappedBatchLoader((ids, env) -> {
                    Map<UUID, DepartmentDto> map = departmentService.findByIds(List.copyOf(ids));
                    return Mono.just(map);
                });
    }
}
