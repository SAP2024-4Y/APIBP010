package com.hospital.appointment.dto;

import java.time.OffsetDateTime;

public record SuggestionDto(
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}
