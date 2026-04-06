package ai.local.nalbbun.api.dto.prompt;

import java.time.LocalDateTime;

public record PromptTemplateRecordDto(
        Long id,
        String name,
        String description,
        String pageScope,
        String category,
        String systemPrompt,
        boolean active,
        boolean defaultPrompt,
        int versionNo,
        Long previousVersionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
