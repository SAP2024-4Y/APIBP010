package com.hospital.doctor.graphql;

import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.service.DoctorService;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DoctorDataLoader {

    public DoctorDataLoader(BatchLoaderRegistry registry, DoctorService doctorService) {
        registry.<UUID, DoctorDto>forTypePair(UUID.class, DoctorDto.class)
                .withName("doctorLoader")
                .registerMappedBatchLoader((ids, env) -> {
                    Map<UUID, DoctorDto> map = doctorService.findByIds(List.copyOf(ids));
                    return Mono.just(map);
                });
    }
}
