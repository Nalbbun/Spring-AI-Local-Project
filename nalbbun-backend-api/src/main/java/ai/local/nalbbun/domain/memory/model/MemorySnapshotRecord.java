package ai.local.nalbbun.domain.memory.model;

import java.time.LocalDateTime;

public record MemorySnapshotRecord(
        Long snapshotId,
        String conversationId,
        String label,
        ConversationMemorySnapshot snapshot,
        LocalDateTime createdAt
) {
}
