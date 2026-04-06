package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.prompt.PromptTemplateRecordDto;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateRecord;

public final class PromptTemplateDtoMapper {
    private PromptTemplateDtoMapper() {}

    public static PromptTemplateRecordDto toDto(PromptTemplateRecord record) {
        return new PromptTemplateRecordDto(
                record.id(),
                record.name(),
                record.description(),
                record.pageScope() == null ? null : record.pageScope().name(),
                record.category() == null ? null : record.category().name(),
                record.systemPrompt(),
                record.active(),
                record.defaultPrompt(),
                record.versionNo(),
                record.previousVersionId(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
