package ai.local.nalbbun.rag.trace;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;

@Builder
public record RagTraceEntry(
        long sequence,
        String traceId,
        String operation,
        String stage,
        String status,
        String message,
        Map<String, Object> details,
        LocalDateTime timestamp
) {
}
