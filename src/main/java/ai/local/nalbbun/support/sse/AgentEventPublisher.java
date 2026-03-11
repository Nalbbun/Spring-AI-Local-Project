package ai.local.nalbbun.support.sse;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AgentEventPublisher {

    private final SseEmitterHelper sseEmitterHelper;

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