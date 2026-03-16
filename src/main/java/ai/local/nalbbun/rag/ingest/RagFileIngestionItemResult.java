package ai.local.nalbbun.rag.ingest;

public record RagFileIngestionItemResult(
        String fileId,
        String fileName,
        String originalFileName,
        String contentType,
        String source,
        String version,
        String title,
        int chunkCount,
        boolean stored,
        String message
) {
    public RagFileIngestionItemResult(String fileName,
                                      String source,
                                      String version,
                                      String title,
                                      int chunkCount,
                                      boolean stored,
                                      String message) {
        this(null, fileName, fileName, null, source, version, title, chunkCount, stored, message);
    }
}
