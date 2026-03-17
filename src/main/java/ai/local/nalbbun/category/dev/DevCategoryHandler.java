package ai.local.nalbbun.category.dev;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.CategoryResponseGenerator;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.service.RagSupportService;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * DevCategoryHandler는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: dev category handler 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class DevCategoryHandler implements CategoryHandler {

    /** parser 값을 보관한다. */
    private final DevCategoryParser parser;
    /** responseGenerator 값을 보관한다. */
    private final CategoryResponseGenerator responseGenerator;
    /** memoryService 값을 보관한다. */
    private final ConversationMemoryService memoryService;
    /** categoryMemoryUpdater 값을 보관한다. */
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;
    /** ragSupportService 값을 보관한다. */
    private final RagSupportService ragSupportService;

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
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
        agentEventPublisher.send(emitter, "DevCategoryHandler", "running", "DEV 질문 분석 중...");

        DevContext context = parser.parse(state);
        state.setCategoryContext(context);

        String parsedSummary = String.format(
                "taskType=%s, topic=%s, stack=%s, parser=%s",
                context.getTaskType(),
                context.getTopic(),
                context.getStackKeywords(),
                context.getParserMode()
        );

        agentEventPublisher.send(
                emitter,
                "ModelTrace-DEV",
                "info",
                "response=" + responseGenerator.describeModel(ChatCategory.DEV)
        );

        RagContext ragContext = ragSupportService.buildContext(ChatCategory.DEV, state.getUserQuery());
        agentEventPublisher.send(
                emitter,
                "RAG-DEV",
                ragContext.isApplied() ? "applied" : (ragContext.isEnabled() ? "empty" : "disabled"),
                ragContext.getTraceMessage()
        );

        String response = responseGenerator.generate(
                ChatCategory.DEV,
                """
                당신은 실무 중심의 개발/인프라/리팩토링 기술 어시스턴트입니다.
                응답은 우선순위와 단계 순서가 보이도록 작성하세요.
                최근 대화의 연속성과 이전 구조 결정을 반영하세요.
                필요하면 선택지보다 권장안을 먼저 제시하세요.
                """,
                parsedSummary,
                state,
                ragContext.getPromptBlock()
        );

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.DEV, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(
                emitter,
                "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage()
        );

        agentEventPublisher.send(emitter, "DevCategoryHandler", "complete", "DEV 응답 생성 완료");

        return CategoryResult.builder()
                .finalResponse(response)
                .payload(context)
                .build();
    }
}