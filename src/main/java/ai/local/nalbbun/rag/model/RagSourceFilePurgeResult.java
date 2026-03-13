package ai.local.nalbbun.rag.model;

import lombok.Builder;

@Builder
public record RagSourceFilePurgeResult(
        String category,
        String source,
        String version,
        String fileId,
        int estimatedVectorRowsBefore,
        int estimatedVectorRowsAfter,
        boolean registryDeleted
) {
}
