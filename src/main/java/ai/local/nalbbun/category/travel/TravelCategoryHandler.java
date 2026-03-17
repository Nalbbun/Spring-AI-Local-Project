package ai.local.nalbbun.category.travel;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TravelCategoryHandler는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: travel category handler 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class TravelCategoryHandler implements CategoryHandler {

    /** parser 값을 보관한다. */
    private final TravelCategoryParser parser;
    /** travelWorkflow 값을 보관한다. */
    private final TravelWorkflow travelWorkflow;
    /** memoryService 값을 보관한다. */
    private final ConversationMemoryService memoryService;
    /** categoryMemoryUpdater 값을 보관한다. */
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    /**
     * 요청 또는 상태를 처리한다.
     *
     * @param state 현재 처리 상태 정보
     * @param emitter SSE 이벤트 전송 객체
     * @return CategoryResult 타입의 처리 결과
     */
    @Override
    public CategoryResult handle(ConversationState state, SseEmitter emitter) {
        TravelContext context = parser.parse(state);
        state.setCategoryContext(context);

        String response = travelWorkflow.execute(state, context, emitter);

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.TRAVEL, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(
                emitter,
                "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage()
        );

        return CategoryResult.builder()
                .finalResponse(response)
                .payload(context)
                .build();
    }
}