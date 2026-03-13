package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;

@Builder
public record RagSourceReindexResult(
        String category,
        String source,
        String version,
        String targetVersion,
        int totalCandidates,
        int successCount,
        List<RagSourceReindexItemResult> results
) {
}
