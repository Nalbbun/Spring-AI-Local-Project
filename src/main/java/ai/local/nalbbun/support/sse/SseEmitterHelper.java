package ai.local.nalbbun.support.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

/**
 * SseEmitterHelper는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: sse emitter helper 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Component
public class SseEmitterHelper {

    /** UTF8_TEXT 값을 보관한다. */
    private static final MediaType UTF8_TEXT =
            new MediaType("text", "plain", StandardCharsets.UTF_8);

    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 대상 정보를 외부로 전송한다.
     *
     * @param emitter SSE 이벤트 전송 객체
     * @param eventName eventName 값
     * @param data data 값
     */
    public void send(SseEmitter emitter, String eventName, Object data) {
        try {
            String payload;

            if (data instanceof String str) {
                payload = str;
            } else {
                payload = objectMapper.writeValueAsString(data);
            }

            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(payload, UTF8_TEXT)
            );
        } catch (Exception e) {
            log.warn("SSE send failed. eventName={}, message={}", eventName, e.getMessage());
            throw new RuntimeException("SSE send failed", e);
        }
    }

    /**
     * 처리를 완료 상태로 반영한다.
     *
     * @param emitter SSE 이벤트 전송 객체
     */
    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("SSE complete failed: {}", e.getMessage());
        }
    }

    /**
     * 처리를 완료 상태로 반영한다.
     *
     * @param emitter SSE 이벤트 전송 객체
     * @param throwable throwable 값
     */
    public void completeWithError(SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception e) {
            log.warn("SSE completeWithError failed: {}", e.getMessage());
        }
    }
}