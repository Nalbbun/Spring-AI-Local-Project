package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.conversation.*;
import ai.local.nalbbun.domain.memory.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
