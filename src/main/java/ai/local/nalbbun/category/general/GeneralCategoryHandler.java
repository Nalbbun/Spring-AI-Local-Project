package ai.local.nalbbun.category.general;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.CategoryResponseGenerator;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * General Category Handler 타입이다.
 *
 * <p>기능 설명: 카테고리 또는 기능별 요청 처리 진입점을 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class GeneralCategoryHandler implements CategoryHandler {

    private final GeneralCategoryParser parser;
    private final CategoryResponseGenerator responseGenerator;
    private final ConversationMemoryService memoryService;
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    private final AgentEventPublisher agentEventPublisher;
    private final SseEmitterHelper sseEmitterHelper;

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    /**
     * handle 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public CategoryResult handle(ConversationState state, SseEmitter emitter) {
        agentEventPublisher.send(emitter, "GeneralCategoryHandler", "running", "GENERAL 질문 해석 중...");

        GeneralContext context = parser.parse(state);
        state.setCategoryContext(context);

        String parsedSummary = String.format("intent=%s", context.getIntent());

        agentEventPublisher.send(
                emitter,
                "ModelTrace-GENERAL",
                "info",
                "response=" + responseGenerator.describeModel(ChatCategory.GENERAL)
        );

        String response = responseGenerator.generateStreaming(
                ChatCategory.GENERAL,
                """
                당신은 일반 질의응답 어시스턴트입니다.
                최근 대화와 중요 메모를 참고하되, 현재 질문에 직접적으로 답변하세요.
                불필요하게 과거 내용을 반복하지 말고 자연스럽게 이어서 답변하세요.
                """,
                parsedSummary,
                state,
                token -> sseEmitterHelper.send(emitter, SseEventNames.TOKEN, token)
        );

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.GENERAL, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(
                emitter,
                "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage()
        );

        agentEventPublisher.send(emitter, "GeneralCategoryHandler", "complete", "GENERAL 응답 생성 완료");

        return CategoryResult.builder()
                .finalResponse(response)
                .payload(context)
                .build();
    }
}