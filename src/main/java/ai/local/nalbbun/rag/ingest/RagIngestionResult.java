package ai.local.nalbbun.rag.ingest;

public record RagIngestionResult(
        String category,
        String source,
        String version,
        String title,
        int chunkCount,
        boolean stored,
        String traceId
) {
}
