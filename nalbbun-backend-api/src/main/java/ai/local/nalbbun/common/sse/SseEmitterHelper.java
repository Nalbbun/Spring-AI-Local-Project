package ai.local.nalbbun.common.sse;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Sse Emitter Helper 타입이다.
 */
@Slf4j
@Component
public class SseEmitterHelper {

    private static final MediaType UTF8_TEXT =
            new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SseDiagnosticsTracker sseDiagnosticsTracker;

    public SseEmitterHelper(SseDiagnosticsTracker sseDiagnosticsTracker) {
        this.sseDiagnosticsTracker = sseDiagnosticsTracker;
    }

    public void send(SseEmitter emitter, String eventName, Object data) {
        try {
            String payload;

            if (data instanceof String str) {
                payload = str;
            } else {
                payload = objectMapper.writeValueAsString(data);
            }

            sseDiagnosticsTracker.updateLastEvent(emitter, eventName, payload);
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(payload, UTF8_TEXT)
            );

            if (!SseEventNames.TOKEN.equals(eventName)) {
                var context = sseDiagnosticsTracker.get(emitter);
                log.info("SSE event sent. conversationId={}, eventName={}, payloadPreview={}",
                        context != null ? context.conversationId() : "unknown",
                        eventName,
                        preview(payload));
            }
        } catch (Exception e) {
            var context = sseDiagnosticsTracker.get(emitter);
            log.error("SSE send failed. conversationId={}, eventName={}, lastEvent={}, payloadPreview={}",
                    context != null ? context.conversationId() : "unknown",
                    eventName,
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    preview(data),
                    e);
            throw new RuntimeException("SSE send failed", e);
        }
    }

    public void complete(SseEmitter emitter) {
        try {
            sseDiagnosticsTracker.markLifecycle(emitter, "complete", null);
            emitter.complete();
        } catch (Exception e) {
            var context = sseDiagnosticsTracker.get(emitter);
            log.warn("SSE complete failed. conversationId={}, lastEvent={}, message={}",
                    context != null ? context.conversationId() : "unknown",
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    e.getMessage());
        }
    }

    public void completeWithError(SseEmitter emitter, Throwable throwable) {
        try {
            sseDiagnosticsTracker.markLifecycle(emitter, "complete-with-error",
                    throwable != null ? throwable.getClass().getSimpleName() + ": " + throwable.getMessage() : "unknown");
            emitter.completeWithError(throwable);
        } catch (Exception e) {
            var context = sseDiagnosticsTracker.get(emitter);
            log.warn("SSE completeWithError failed. conversationId={}, lastEvent={}, message={}",
                    context != null ? context.conversationId() : "unknown",
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    e.getMessage());
        }
    }

    private String preview(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value).replace("\n", " ").replace("\r", " ").trim();
        return raw.length() > 220 ? raw.substring(0, 220) + "..." : raw;
    }
}
