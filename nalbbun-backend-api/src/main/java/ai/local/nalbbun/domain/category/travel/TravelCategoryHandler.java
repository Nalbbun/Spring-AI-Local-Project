package ai.local.nalbbun.domain.category.travel;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.common.sse.AgentEventPublisher;
import ai.local.nalbbun.domain.agent.application.travel.TravelWorkflow;
import ai.local.nalbbun.domain.category.CategoryHandler;
import ai.local.nalbbun.domain.category.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.domain.category.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.domain.category.model.CategoryResult;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;

/**
 * Travel Category Handler 타입이다.
 *
 * <p>기능 설명: 카테고리 또는 기능별 요청 처리 진입점을 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class TravelCategoryHandler implements CategoryHandler {

    private final TravelCategoryParser parser;
    private final TravelWorkflow travelWorkflow;
    private final ConversationMemoryService memoryService;
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    private final AgentEventPublisher agentEventPublisher;

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    /**
     * handle 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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