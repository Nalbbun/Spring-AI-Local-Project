package ai.local.nalbbun.api.dto.conversation;

import java.time.LocalDateTime;

public record ConversationMessageDto(String role, String content, String category, LocalDateTime createdAt) {}
