package ai.local.nalbbun.api.dto.prompt;

import java.time.LocalDateTime;

public record PromptEntryHistoryDto(
        Long historyId,
        String promptId,
        String action,
        String name,
        String category,
        String systemPrompt,
        String description,
        boolean isDefault,
        boolean active,
        int versionNo,
        String previousVersionId,
        LocalDateTime capturedAt
) {
}
