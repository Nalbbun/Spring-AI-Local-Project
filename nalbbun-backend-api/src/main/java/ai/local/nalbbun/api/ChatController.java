package ai.local.nalbbun.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.common.sse.SseDiagnosticsTracker;
import ai.local.nalbbun.common.sse.SseEmitterHelper;
import ai.local.nalbbun.common.sse.SseEventNames;
import ai.local.nalbbun.domain.category.model.CategoryResult;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.chat.application.CategoryChatOrchestrator;
import ai.local.nalbbun.domain.conversation.ConversationIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@Slf4j
@RestController
public class ChatController {

    private final CategoryChatOrchestrator categoryChatOrchestrator;
    private final SseEmitterHelper sseEmitterHelper;
    private final ConversationIdResolver conversationIdResolver;
    private final SseDiagnosticsTracker sseDiagnosticsTracker;
    private final Executor chatTaskExecutor;

    public ChatController(
            CategoryChatOrchestrator categoryChatOrchestrator,
            SseEmitterHelper sseEmitterHelper,
            ConversationIdResolver conversationIdResolver,
            SseDiagnosticsTracker sseDiagnosticsTracker,
            @Qualifier("chatTaskExecutor") Executor chatTaskExecutor
    ) {
        this.categoryChatOrchestrator = categoryChatOrchestrator;
        this.sseEmitterHelper = sseEmitterHelper;
        this.conversationIdResolver = conversationIdResolver;
        this.sseDiagnosticsTracker = sseDiagnosticsTracker;
        this.chatTaskExecutor = chatTaskExecutor;
    }

    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(name = "message") String message,
            @RequestParam(name = "category", required = false) ChatCategory category,
            @RequestParam(name = "promptId", required = false) String promptId,
            HttpServletRequest request,
            HttpSession session
    ) {
        String conversationId = conversationIdResolver.resolve(request, session);
        long timeoutMs = 300000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);
        String requestSummary = "category=" + (category != null ? category : "AUTO")
                + ", promptId=" + (promptId != null ? promptId : "")
                + ", messagePreview=" + summarize(message);

        sseDiagnosticsTracker.register(emitter, conversationId, requestSummary);
        sseDiagnosticsTracker.markLifecycle(emitter, "opened", null);
        log.info("SSE stream opened. conversationId={}, timeoutMs={}, {}",
                conversationId, timeoutMs, requestSummary);

        emitter.onCompletion(() -> {
            var context = sseDiagnosticsTracker.get(emitter);
            log.warn("SSE stream completed. conversationId={}, lifecycle={}, lastEvent={}, lastPreview={}",
                    conversationId,
                    context != null ? context.lifecycle() : "unknown",
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    context != null && context.lastEvent() != null ? context.lastEvent().preview() : "");
            sseDiagnosticsTracker.remove(emitter);
        });

        emitter.onTimeout(() -> {
            sseDiagnosticsTracker.markLifecycle(emitter, "timeout", null);
            var context = sseDiagnosticsTracker.get(emitter);
            log.error("SSE stream timeout. conversationId={}, lastEvent={}, lastPreview={}, requestSummary={}",
                    conversationId,
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    context != null && context.lastEvent() != null ? context.lastEvent().preview() : "",
                    requestSummary);
        });

        emitter.onError(ex -> {
            sseDiagnosticsTracker.markLifecycle(emitter, "error",
                    ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : "unknown");
            var context = sseDiagnosticsTracker.get(emitter);
            log.error("SSE emitter error. conversationId={}, lastEvent={}, lastPreview={}, requestSummary={}",
                    conversationId,
                    context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                    context != null && context.lastEvent() != null ? context.lastEvent().preview() : "",
                    requestSummary,
                    ex);
        });

        CompletableFuture.runAsync(() -> {
            try {
                log.info("SSE worker started. conversationId={}, {}", conversationId, requestSummary);
                CategoryResult result = categoryChatOrchestrator.execute(
                        message,
                        conversationId,
                        category,
                        promptId,
                        emitter
                );
                log.info("SSE worker completed category handling. conversationId={}, resolvedResponseLength={}",
                        conversationId,
                        result != null && result.getFinalResponse() != null ? result.getFinalResponse().length() : 0);

                sseEmitterHelper.send(emitter, SseEventNames.COMPLETE, "");
                sseDiagnosticsTracker.markLifecycle(emitter, "complete-dispatched", null);
                sseEmitterHelper.complete(emitter);

            } catch (Exception e) {
                var context = sseDiagnosticsTracker.get(emitter);
                log.error("SSE worker failed. conversationId={}, lastEvent={}, lastPreview={}, requestSummary={}",
                        conversationId,
                        context != null && context.lastEvent() != null ? context.lastEvent().name() : "none",
                        context != null && context.lastEvent() != null ? context.lastEvent().preview() : "",
                        requestSummary,
                        e);
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                try {
                    sseEmitterHelper.send(emitter, SseEventNames.ERROR, errorMessage);
                } catch (Exception sendException) {
                    log.error("SSE error event dispatch failed. conversationId={}", conversationId, sendException);
                }
                sseDiagnosticsTracker.markLifecycle(emitter, "failed", errorMessage);
                sseEmitterHelper.complete(emitter);
            }
        }, chatTaskExecutor).orTimeout(timeoutMs + 10000L, TimeUnit.MILLISECONDS).exceptionally(ex -> {
            log.error("SSE worker future failed outside stream loop. conversationId={}, requestSummary={}",
                    conversationId, requestSummary, ex);
            return null;
        });

        return emitter;
    }

    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\n", " ").replace("\r", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }
}
