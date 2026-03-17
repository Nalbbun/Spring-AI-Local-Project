package ai.local.nalbbun.rag.trace;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;

/**
 * RagTraceEntry는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag trace entry 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param sequence sequence 값
 * @param traceId traceId 식별자 값
 * @param operation operation 값
 * @param stage stage 값
 * @param status status 값
 * @param message 사용자 입력 또는 질의 내용
 * @param details details 값
 * @param timestamp timestamp 값
 */
@Builder
public record RagTraceEntry(
        long sequence,
        String traceId,
        String operation,
        String stage,
        String status,
        String message,
        Map<String, Object> details,
        LocalDateTime timestamp
) {
}
