package ai.local.nalbbun.rag.trace;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record RagTraceSummary(
        String traceId,
        String operation,
        String finalStatus,
        String lastStage,
        String lastMessage,
        int entryCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
