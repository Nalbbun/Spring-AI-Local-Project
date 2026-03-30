package ai.local.nalbbun.api.dto.conversation;

import java.time.LocalDateTime;

public record ImportantNoteDto(String category, String note, LocalDateTime createdAt) {}
