package ai.local.nalbbun.api.dto.prompt;

import java.time.LocalDateTime;

public record PromptEntryDto(
        String id,
        String name,
        String category,
        String systemPrompt,
        String description,
        boolean isDefault,
        boolean active,
        int versionNo,
        String previousVersionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
