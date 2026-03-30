package ai.local.nalbbun.api.dto.apikey;

import java.time.LocalDateTime;

public record ApiKeyEntryDto(
        String id,
        String provider,
        String label,
        String description,
        String maskedKey,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
