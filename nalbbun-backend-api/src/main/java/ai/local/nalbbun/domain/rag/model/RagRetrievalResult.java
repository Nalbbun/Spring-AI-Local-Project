package ai.local.nalbbun.domain.rag.model;

import java.util.List;

/**
 * RAG 검색 결과와 진단 정보를 함께 표현한다.
 */
public record RagRetrievalResult(
        List<RagRetrievedDocument> documents,
        int candidateCount,
        int returnedCount,
        long elapsedMs,
        String filterExpression,
        double similarityThreshold,
        int topK,
        boolean rerankApplied,
        String retrievalMode
) {
    public static RagRetrievalResult empty(String filterExpression, double similarityThreshold, int topK, long elapsedMs, String retrievalMode) {
        return new RagRetrievalResult(List.of(), 0, 0, elapsedMs, filterExpression, similarityThreshold, topK, false, retrievalMode);
    }
}
