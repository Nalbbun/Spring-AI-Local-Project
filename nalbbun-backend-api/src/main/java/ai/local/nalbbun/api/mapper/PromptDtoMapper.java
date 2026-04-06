package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.prompt.PromptEntryDto;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;

public final class PromptDtoMapper {
    private PromptDtoMapper() {}

    public static PromptEntryDto toDto(PromptEntry entry) {
        return new PromptEntryDto(
                entry.getId(),
                entry.getName(),
                entry.getCategory() == null ? null : entry.getCategory().name(),
                entry.getSystemPrompt(),
                entry.getDescription(),
                entry.isDefault(),
                entry.isActive(),
                entry.getVersionNo(),
                entry.getPreviousVersionId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
