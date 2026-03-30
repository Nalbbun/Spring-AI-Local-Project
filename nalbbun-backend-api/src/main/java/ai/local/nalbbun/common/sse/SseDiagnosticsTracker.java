package ai.local.nalbbun.common.sse;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 스트림의 진단 정보를 추적한다.
 */
@Component
public class SseDiagnosticsTracker {

    private final Map<SseEmitter, SseDiagnosticsContext> contexts = new ConcurrentHashMap<>();

    public void register(SseEmitter emitter, String conversationId, String requestSummary) {
        contexts.put(emitter, new SseDiagnosticsContext(conversationId, requestSummary, Instant.now(), "created", null));
    }

    public void updateLastEvent(SseEmitter emitter, String eventName, Object payloadPreview) {
        contexts.computeIfPresent(emitter, (key, current) -> current.withLastEvent(eventName, payloadPreview));
    }

    public void markLifecycle(SseEmitter emitter, String lifecycle, Object payloadPreview) {
        contexts.computeIfPresent(emitter, (key, current) -> current.withLifecycle(lifecycle, payloadPreview));
    }

    public SseDiagnosticsContext get(SseEmitter emitter) {
        return contexts.get(emitter);
    }

    public void remove(SseEmitter emitter) {
        contexts.remove(emitter);
    }

    public record SseDiagnosticsContext(
            String conversationId,
            String requestSummary,
            Instant createdAt,
            String lifecycle,
            EventSnapshot lastEvent
    ) {
        SseDiagnosticsContext withLastEvent(String eventName, Object payloadPreview) {
            return new SseDiagnosticsContext(
                    conversationId,
                    requestSummary,
                    createdAt,
                    lifecycle,
                    new EventSnapshot(eventName, summarize(payloadPreview), Instant.now())
            );
        }

        SseDiagnosticsContext withLifecycle(String nextLifecycle, Object payloadPreview) {
            EventSnapshot snapshot = lastEvent;
            if (payloadPreview != null) {
                snapshot = new EventSnapshot(
                        payloadPreview instanceof String s ? s : "lifecycle",
                        summarize(payloadPreview),
                        Instant.now()
                );
            }
            return new SseDiagnosticsContext(conversationId, requestSummary, createdAt, nextLifecycle, snapshot);
        }

        private static String summarize(Object payloadPreview) {
            if (payloadPreview == null) {
                return null;
            }
            String raw = String.valueOf(payloadPreview).replace("\n", " ").replace("\r", " ").trim();
            return raw.length() > 220 ? raw.substring(0, 220) + "..." : raw;
        }
    }

    public record EventSnapshot(String name, String preview, Instant at) {
    }
}
