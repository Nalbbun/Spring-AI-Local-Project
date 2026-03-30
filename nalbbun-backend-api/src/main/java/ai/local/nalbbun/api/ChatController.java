package ai.local.nalbbun.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.CategoryResult;
import ai.local.nalbbun.domain.chat.application.CategoryChatOrchestrator;
import ai.local.nalbbun.domain.conversation.ConversationIdResolver;
import ai.local.nalbbun.common.sse.SseEmitterHelper;
import ai.local.nalbbun.common.sse.SseEventNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Chat Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@RestController
public class ChatController {

    private final CategoryChatOrchestrator categoryChatOrchestrator;
    private final SseEmitterHelper sseEmitterHelper;
    private final ConversationIdResolver conversationIdResolver;
    private final Executor chatTaskExecutor;

    /**
     * Chat Controller 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * stream 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(name = "message") String message,
            @RequestParam(name = "category", required = false) ChatCategory category,
            @RequestParam(name = "promptId", required = false) String promptId,
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
                        promptId,
                        emitter
                );

                // 핸들러 내부에서 TOKEN 이벤트를 직접 전송하므로
                // 여기서는 완성된 전체 응답을 message 이벤트로 추가 전송 (선택적 활용 가능)
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
