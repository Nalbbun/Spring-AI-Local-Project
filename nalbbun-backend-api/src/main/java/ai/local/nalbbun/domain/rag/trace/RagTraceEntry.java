package ai.local.nalbbun.domain.rag.trace;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;

/**
 * Rag Trace Entry 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 불변 데이터 전달 객체로 사용된다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
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
