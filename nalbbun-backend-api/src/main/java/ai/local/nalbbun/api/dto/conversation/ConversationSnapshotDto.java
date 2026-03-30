package ai.local.nalbbun.api.dto.conversation;

import java.util.List;
import java.util.Map;

public record ConversationSnapshotDto(
        String conversationId,
        List<ConversationMessageDto> recentMessages,
        Map<String, ConversationSummaryDto> categorySummaries,
        List<ImportantNoteDto> importantNotes
) {}
