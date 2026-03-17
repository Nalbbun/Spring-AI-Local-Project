package ai.local.nalbbun.controller.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.orchestrator.CategoryChatOrchestrator;
import ai.local.nalbbun.service.conversation.ConversationIdResolver;
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * ChatController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: chat controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
public class ChatController {

    /** categoryChatOrchestrator 값을 보관한다. */
    private final CategoryChatOrchestrator categoryChatOrchestrator;
    /** sseEmitterHelper 값을 보관한다. */
    private final SseEmitterHelper sseEmitterHelper;
    /** conversationIdResolver 값을 보관한다. */
    private final ConversationIdResolver conversationIdResolver;
    /** chatTaskExecutor 값을 보관한다. */
    private final Executor chatTaskExecutor;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param categoryChatOrchestrator categoryChatOrchestrator 값
     * @param sseEmitterHelper SSE 응답 객체
     * @param conversationIdResolver conversationIdResolver 값
     * @param chatTaskExecutor chatTaskExecutor 값
     */
    public ChatController(
            CategoryChatOrchestrator categoryChatOrchestrator,
            SseEmitterHelper sseEmitterHelper,
            ConversationIdResolver conversationIdResolver,
            @Qualifier("chatTaskExecutor") Executor chatTaskExecutor
    ) {
        this.categoryChatOrchestrator = categoryChatOrchestrator;
        this.sseEmitterHelper = sseEmitterHelper;
        this.conversationIdResolver = conversationIdResolver;
        this.chatTaskExecutor = chatTaskExecutor;
    }

    /**
     * stream 기능을 수행한다.
     *
     * @param message 사용자 입력 또는 질의 내용
     * @param category 대상 카테고리 정보
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return SSE 응답 스트림 객체
     */
    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(name = "message") String message,
            @RequestParam(name = "category", required = false) ChatCategory category,
            HttpServletRequest request,
            HttpSession session
    ) {
        String conversationId = conversationIdResolver.resolve(request, session);
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
        }, chatTaskExecutor);

        return emitter;
    }
}
