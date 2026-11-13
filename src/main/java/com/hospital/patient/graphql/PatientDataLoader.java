package com.hospital.patient.graphql;

import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.service.PatientService;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PatientDataLoader {

    public PatientDataLoader(BatchLoaderRegistry registry, PatientService patientService) {
        registry.<UUID, PatientDto>forTypePair(UUID.class, PatientDto.class)
                .withName("patientLoader")
                .registerMappedBatchLoader((ids, env) -> {
                    Map<UUID, PatientDto> map = patientService.findByIds(List.copyOf(ids));
                    return Mono.just(map);
                });
    }
}
