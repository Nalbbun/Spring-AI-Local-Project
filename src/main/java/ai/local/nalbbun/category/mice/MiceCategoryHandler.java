package ai.local.nalbbun.category.mice;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import lombok.RequiredArgsConstructor;

/**
 * Mice Category Handler 타입이다.
 *
 * <p>기능 설명: 카테고리 또는 기능별 요청 처리 진입점을 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class MiceCategoryHandler implements CategoryHandler {

    private final MiceCategoryParser parser;
    private final CategoryResponseGenerator responseGenerator;
    private final ConversationMemoryService memoryService;
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    private final AgentEventPublisher agentEventPublisher;
    private final RagSupportService ragSupportService;
    private final SseEmitterHelper sseEmitterHelper;

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    /**
     * handle 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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

        String response = responseGenerator.generateStreaming(
                ChatCategory.MICE,
                """
                당신은 MICE/행사기획 전문 어시스턴트입니다.
                답변은 배경-목표-방향-구성 순으로 정리하고,
                이전 대화에서 정리된 메시지, 구조, 방향성을 이어받아 일관성 있게 작성하세요.
                필요시 슬로건, 기획 의도, 운영 포인트를 구조적으로 제시하세요.
                """,
                parsedSummary,
                state,
                ragContext.getPromptBlock(),
                token -> sseEmitterHelper.send(emitter, SseEventNames.TOKEN, token)
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