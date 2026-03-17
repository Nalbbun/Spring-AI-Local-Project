package ai.local.nalbbun.support.sse;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

/**
 * AgentEventPublisher는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: agent event publisher 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class AgentEventPublisher {

    /** sseEmitterHelper 값을 보관한다. */
    private final SseEmitterHelper sseEmitterHelper;

    /**
     * 대상 정보를 외부로 전송한다.
     *
     * @param emitter SSE 이벤트 전송 객체
     * @param agent agent 값
     * @param status status 값
     * @param message 사용자 입력 또는 질의 내용
     */
    public void send(SseEmitter emitter, String agent, String status, String message) {
        if (emitter == null) {
            return;
        }

        sseEmitterHelper.send(
                emitter,
                SseEventNames.AGENT,
                Map.of(
                        "agent", agent,
                        "status", status,
                        "message", message
                )
        );
    }
}