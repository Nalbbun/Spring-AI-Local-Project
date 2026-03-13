package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.rag.ingest.RagIngestionResult;
import lombok.Builder;

@Builder
public record RagSourceReindexItemResult(
        String source,
        String version,
        String targetVersion,
        String title,
        String storageKind,
        boolean success,
        String detail,
        RagIngestionResult ingestionResult
) {
}
