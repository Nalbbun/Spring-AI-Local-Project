package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.prompt.PromptEntryDto;
import ai.local.nalbbun.api.dto.prompt.PromptEntryHistoryDto;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.model.PromptEntryHistoryRecord;

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

    public static PromptEntryHistoryDto toHistoryDto(PromptEntryHistoryRecord record) {
        return new PromptEntryHistoryDto(
                record.historyId(),
                record.promptId(),
                record.action(),
                record.name(),
                record.category() == null ? null : record.category().name(),
                record.systemPrompt(),
                record.description(),
                record.isDefault(),
                record.active(),
                record.versionNo(),
                record.previousVersionId(),
                record.capturedAt()
        );
    }
}
