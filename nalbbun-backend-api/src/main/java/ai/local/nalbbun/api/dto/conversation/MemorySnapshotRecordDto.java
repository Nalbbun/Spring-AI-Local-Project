package ai.local.nalbbun.api.dto.conversation;

import java.time.LocalDateTime;

public record MemorySnapshotRecordDto(
        Long snapshotId,
        String conversationId,
        String label,
        ConversationSnapshotDto snapshot,
        LocalDateTime createdAt
) {
}
