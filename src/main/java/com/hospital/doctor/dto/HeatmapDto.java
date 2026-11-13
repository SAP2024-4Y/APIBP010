package com.hospital.doctor.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record HeatmapDto(
        UUID doctorId,
        String week,
        Map<String, List<Integer>> grid
) {}
