package ai.local.nalbbun.common.sse;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AgentEventPublisher {

    private final SseEmitterHelper sseEmitterHelper;

    public void send(SseEmitter emitter, String agent, String status, String message) {
        sendDetails(emitter, agent, status, message, Map.of());
    }

    public void sendDetails(SseEmitter emitter, String agent, String status, String message, Map<String, Object> details) {
        if (emitter == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agent", agent);
        payload.put("status", status);
        payload.put("message", message);
        if (details != null && !details.isEmpty()) {
            payload.putAll(details);
        }
        sseEmitterHelper.send(emitter, SseEventNames.AGENT, payload);
    }
}
