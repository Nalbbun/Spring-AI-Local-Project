package ai.local.nalbbun.category.mice;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.CategoryResponseGenerator;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.mice.model.MiceContext;
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
 * MiceCategoryHandler는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: mice category handler 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MiceCategoryHandler implements CategoryHandler {

    /** parser 값을 보관한다. */
    private final MiceCategoryParser parser;
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
        return ChatCategory.MICE;
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
        agentEventPublisher.send(emitter, "MiceCategoryHandler", "running", "MICE 질문 분석 중...");

        MiceContext context = parser.parse(state);
        state.setCategoryContext(context);

        String parsedSummary = String.format(
                "eventType=%s, deliverable=%s, targetRegion=%s, parser=%s",
                context.getEventType(),
                context.getDeliverableType(),
                context.getTargetRegion(),
                context.getParserMode()
        );

        agentEventPublisher.send(
                emitter,
                "ModelTrace-MICE",
                "info",
                "response=" + responseGenerator.describeModel(ChatCategory.MICE)
        );

        RagContext ragContext = ragSupportService.buildContext(ChatCategory.MICE, state.getUserQuery());
        agentEventPublisher.send(
                emitter,
                "RAG-MICE",
                ragContext.isApplied() ? "applied" : (ragContext.isEnabled() ? "empty" : "disabled"),
                ragContext.getTraceMessage()
        );

        String response = responseGenerator.generate(
                ChatCategory.MICE,
                """
                당신은 MICE/행사기획 전문 어시스턴트입니다.
                답변은 배경-목표-방향-구성 순으로 정리하고,
                이전 대화에서 정리된 메시지, 구조, 방향성을 이어받아 일관성 있게 작성하세요.
                필요시 슬로건, 기획 의도, 운영 포인트를 구조적으로 제시하세요.
                """,
                parsedSummary,
                state,
                ragContext.getPromptBlock()
        );

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.MICE, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(
                emitter,
                "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage()
        );

        agentEventPublisher.send(emitter, "MiceCategoryHandler", "complete", "MICE 응답 생성 완료");

        return CategoryResult.builder()
                .finalResponse(response)
                .payload(context)
                .build();
    }
}