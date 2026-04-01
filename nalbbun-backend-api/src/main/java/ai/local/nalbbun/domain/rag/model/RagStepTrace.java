package ai.local.nalbbun.domain.rag.model;

/**
 * RAG 단계 추적 정보를 표현한다.
 */
public record RagStepTrace(
        String name,
        String status,
        String message
) {
}
