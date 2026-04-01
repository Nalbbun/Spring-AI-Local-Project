package ai.local.nalbbun.domain.rag.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Rag Context 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 enabled, applied, reason, traceMessage, promptBlock, documents, sourceFilter, versionFilter 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
@Getter
@Builder
public class RagContext {

    private final boolean enabled;
    private final boolean applied;
    private final String reason;
    private final String traceMessage;
    private final String promptBlock;
    private final List<RagRetrievedDocument> documents;
    private final String sourceFilter;
    private final String versionFilter;
    private final List<RagStepTrace> steps;
    private final int candidateCount;
    private final int hitCount;
    private final long retrievalElapsedMs;
    private final String filterExpression;
    private final double similarityThreshold;
    private final int topK;
    private final boolean rerankApplied;
    private final String retrievalMode;

    /**
     * disabled 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public static RagContext disabled(String reason) {
        return RagContext.builder()
                .enabled(false)
                .applied(false)
                .reason(reason)
                .traceMessage("rag=off, reason=" + reason)
                .promptBlock("")
                .documents(List.of())
                .sourceFilter("")
                .versionFilter("")
                .steps(List.of(new RagStepTrace("RAG 상태 확인", "disabled", reason)))
                .candidateCount(0)
                .hitCount(0)
                .retrievalElapsedMs(0L)
                .filterExpression("")
                .similarityThreshold(0.0d)
                .topK(0)
                .rerankApplied(false)
                .retrievalMode("disabled")
                .build();
    }

    /**
     * enabled But Empty 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public static RagContext enabledButEmpty(String reason) {
        return RagContext.builder()
                .enabled(true)
                .applied(false)
                .reason(reason)
                .traceMessage("rag=on, hits=0, reason=" + reason)
                .promptBlock("")
                .documents(List.of())
                .sourceFilter("")
                .versionFilter("")
                .steps(List.of(new RagStepTrace("RAG 상태 확인", "disabled", reason)))
                .candidateCount(0)
                .hitCount(0)
                .retrievalElapsedMs(0L)
                .filterExpression("")
                .similarityThreshold(0.0d)
                .topK(0)
                .rerankApplied(false)
                .retrievalMode("disabled")
                .build();
    }
}
