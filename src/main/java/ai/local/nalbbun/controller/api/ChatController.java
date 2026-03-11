package ai.local.nalbbun.controller.api;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.orchestrator.CategoryChatOrchestrator;
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final CategoryChatOrchestrator categoryChatOrchestrator;
    private final SseEmitterHelper sseEmitterHelper;

    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(name = "message") String message,
            @RequestParam(name = "category", required = false) ChatCategory category,
            HttpSession session
    ) {
        String conversationId = session.getId();
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                CategoryResult result = categoryChatOrchestrator.execute(
                        message,
                        conversationId,
                        category,
                        emitter
                );

                sseEmitterHelper.send(emitter, SseEventNames.MESSAGE, result.getFinalResponse());
                sseEmitterHelper.send(emitter, SseEventNames.COMPLETE, "");
                sseEmitterHelper.complete(emitter);

            } catch (Exception e) {
                sseEmitterHelper.send(emitter, SseEventNames.ERROR, e.getMessage());
                sseEmitterHelper.completeWithError(emitter, e);
            }
        });

        return emitter;
    }
}