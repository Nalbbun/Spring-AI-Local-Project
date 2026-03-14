package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RagContext {

    private final boolean enabled;
    private final boolean applied;
    private final String reason;
    private final String traceId;
    private final String traceMessage;
    private final String promptBlock;
    private final List<RagRetrievedDocument> documents;
    private final String sourceFilter;
    private final String versionFilter;

    public static RagContext disabled(String reason) {
        return RagContext.builder()
                .enabled(false)
                .applied(false)
                .reason(reason)
                .traceId("")
                .traceMessage("rag=off, reason=" + reason)
                .promptBlock("")
                .documents(List.of())
                .sourceFilter("")
                .versionFilter("")
                .build();
    }

    public static RagContext enabledButEmpty(String reason) {
        return RagContext.builder()
                .enabled(true)
                .applied(false)
                .reason(reason)
                .traceId("")
                .traceMessage("rag=on, hits=0, reason=" + reason)
                .promptBlock("")
                .documents(List.of())
                .sourceFilter("")
                .versionFilter("")
                .build();
    }
}
