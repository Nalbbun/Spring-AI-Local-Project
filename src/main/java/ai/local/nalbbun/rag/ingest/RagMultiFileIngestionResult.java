package ai.local.nalbbun.rag.ingest;

import java.util.List;

public record RagMultiFileIngestionResult(
        String category,
        String source,
        String version,
        int requestedFileCount,
        int successCount,
        int failCount,
        int totalChunkCount,
        List<RagFileIngestionItemResult> files
) {
}
