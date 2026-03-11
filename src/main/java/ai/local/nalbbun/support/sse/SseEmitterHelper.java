package ai.local.nalbbun.support.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class SseEmitterHelper {

    private static final MediaType UTF8_TEXT =
            new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("SSE complete failed: {}", e.getMessage());
        }
    }

    public void completeWithError(SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception e) {
            log.warn("SSE completeWithError failed: {}", e.getMessage());
        }
    }
}