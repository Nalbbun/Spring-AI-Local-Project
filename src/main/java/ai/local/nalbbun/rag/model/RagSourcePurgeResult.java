package ai.local.nalbbun.rag.model;

import lombok.Builder;

@Builder
public record RagSourcePurgeResult(
        String category,
        String source,
        String version,
        int estimatedVectorRowsBefore,
        int estimatedVectorRowsAfter,
        int registryEntriesRemoved,
        boolean registryDeleted
) {
}
