package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * RagContext는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag context 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Getter
@Builder
public class RagContext {

    /** enabled 값을 보관한다. */
    private final boolean enabled;
    /** applied 값을 보관한다. */
    private final boolean applied;
    /** reason 값을 보관한다. */
    private final String reason;
    /** traceMessage 값을 보관한다. */
    private final String traceMessage;
    /** promptBlock 값을 보관한다. */
    private final String promptBlock;
    /** documents 값을 보관한다. */
    private final List<RagRetrievedDocument> documents;
    /** sourceFilter 값을 보관한다. */
    private final String sourceFilter;
    /** versionFilter 값을 보관한다. */
    private final String versionFilter;

    /**
     * disabled 기능을 수행한다.
     *
     * @param reason reason 값
     * @return RagContext 타입의 처리 결과
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
                .build();
    }

    /**
     * enabledButEmpty 기능을 수행한다.
     *
     * @param reason reason 값
     * @return RagContext 타입의 처리 결과
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
                .build();
    }
}
