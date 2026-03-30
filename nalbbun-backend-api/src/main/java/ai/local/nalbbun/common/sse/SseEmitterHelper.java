package ai.local.nalbbun.common.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

/**
 * Sse Emitter Helper 타입이다.
 *
 * <p>기능 설명: 반복 보조 로직을 캡슐화한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Slf4j
@Component
public class SseEmitterHelper {

    private static final MediaType UTF8_TEXT =
            new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * send 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * complete 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("SSE complete failed: {}", e.getMessage());
        }
    }

    /**
     * complete With Error 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void completeWithError(SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception e) {
            log.warn("SSE completeWithError failed: {}", e.getMessage());
        }
    }
}