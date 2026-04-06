package ai.local.nalbbun.domain.prompt.model;

import java.time.LocalDateTime;

import ai.local.nalbbun.domain.category.model.ChatCategory;

public record PromptEntryHistoryRecord(
        Long historyId,
        String promptId,
        String action,
        String name,
        ChatCategory category,
        String systemPrompt,
        String description,
        boolean isDefault,
        boolean active,
        int versionNo,
        String previousVersionId,
        LocalDateTime capturedAt
) {
}
