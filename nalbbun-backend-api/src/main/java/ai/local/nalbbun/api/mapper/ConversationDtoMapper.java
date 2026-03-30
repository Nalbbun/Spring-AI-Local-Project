package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.conversation.*;
import ai.local.nalbbun.domain.memory.model.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ConversationDtoMapper {
    private ConversationDtoMapper() {}

    public static ConversationSnapshotDto toDto(ConversationMemorySnapshot snapshot) {
        Map<String, ConversationSummaryDto> summaries = new LinkedHashMap<>();
        if (snapshot.getCategorySummaries() != null) {
            snapshot.getCategorySummaries().forEach((k, v) -> summaries.put(k, toDto(v)));
        }
        return new ConversationSnapshotDto(
                snapshot.getConversationId(),
                snapshot.getRecentMessages() == null ? java.util.List.of() : snapshot.getRecentMessages().stream().map(ConversationDtoMapper::toDto).toList(),
                summaries,
                snapshot.getImportantNotes() == null ? java.util.List.of() : snapshot.getImportantNotes().stream().map(ConversationDtoMapper::toDto).toList()
        );
    }

    public static ConversationListItemDto toListItemDto(ConversationMemorySnapshot snapshot) {
        Set<String> categories = new LinkedHashSet<>();
        LocalDateTime lastUpdated = null;

        if (snapshot.getCategorySummaries() != null) {
            snapshot.getCategorySummaries().forEach((key, summary) -> {
                if (key != null && !key.isBlank()) {
                    categories.add(key);
                }
                if (summary != null && summary.getCategory() != null) {
                    categories.add(summary.getCategory().name());
                }
            });
            for (MemorySummary summary : snapshot.getCategorySummaries().values()) {
                if (summary != null) {
                    lastUpdated = max(lastUpdated, summary.getUpdatedAt());
                }
            }
        }

        int messageCount = 0;
        if (snapshot.getRecentMessages() != null) {
            messageCount = snapshot.getRecentMessages().size();
            for (MemoryMessage message : snapshot.getRecentMessages()) {
                if (message == null) continue;
                if (message.getCategory() != null) {
                    categories.add(message.getCategory().name());
                }
                lastUpdated = max(lastUpdated, message.getCreatedAt());
            }
        }

        if (snapshot.getImportantNotes() != null) {
            for (ImportantNote note : snapshot.getImportantNotes()) {
                if (note == null) continue;
                if (note.getCategory() != null) {
                    categories.add(note.getCategory().name());
                }
                lastUpdated = max(lastUpdated, note.getCreatedAt());
            }
        }

        return new ConversationListItemDto(
                snapshot.getConversationId(),
                java.util.List.copyOf(categories),
                lastUpdated,
                messageCount
        );
    }

    public static ConversationMessageDto toDto(MemoryMessage message) {
        return new ConversationMessageDto(
                message.getRole(),
                message.getContent(),
                message.getCategory() == null ? null : message.getCategory().name(),
                message.getCreatedAt()
        );
    }

    public static ConversationSummaryDto toDto(MemorySummary summary) {
        return new ConversationSummaryDto(
                summary.getCategory() == null ? null : summary.getCategory().name(),
                summary.getSummary(),
                summary.getUpdatedAt()
        );
    }

    public static ImportantNoteDto toDto(ImportantNote note) {
        return new ImportantNoteDto(
                note.getCategory() == null ? null : note.getCategory().name(),
                note.getNote(),
                note.getCreatedAt()
        );
    }

    private static LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }
}
