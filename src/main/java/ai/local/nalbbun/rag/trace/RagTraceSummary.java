package ai.local.nalbbun.rag.trace;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * RagTraceSummary는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag trace summary 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param traceId traceId 식별자 값
 * @param operation operation 값
 * @param finalStatus finalStatus 값
 * @param lastStage lastStage 값
 * @param lastMessage lastMessage 값
 * @param entryCount entryCount 값
 * @param startedAt startedAt 값
 * @param endedAt endedAt 값
 */
@Builder
public record RagTraceSummary(
        String traceId,
        String operation,
        String finalStatus,
        String lastStage,
        String lastMessage,
        int entryCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
