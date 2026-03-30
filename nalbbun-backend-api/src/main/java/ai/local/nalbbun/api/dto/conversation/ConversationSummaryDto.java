package ai.local.nalbbun.api.dto.conversation;

import java.time.LocalDateTime;

public record ConversationSummaryDto(String category, String summary, LocalDateTime updatedAt) {}
