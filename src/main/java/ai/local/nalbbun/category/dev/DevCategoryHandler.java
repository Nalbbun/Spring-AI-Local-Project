package ai.local.nalbbun.category.dev;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.CategoryResponseGenerator;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.CategoryResult;
import ai.local.nalbbun.category.model.ConversationState;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.service.RagSupportService;
import ai.local.nalbbun.memory.service.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import lombok.RequiredArgsConstructor;

/**
 * Dev Category Handler 타입이다.
 *
 * <p>기능 설명: 카테고리 또는 기능별 요청 처리 진입점을 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class DevCategoryHandler implements CategoryHandler {

    private final DevCategoryParser parser;
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
        return ChatCategory.DEV;
    }

    /**
     * handle 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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

        String response = responseGenerator.generateStreaming(
                ChatCategory.DEV,
                """
                당신은 실무 중심의 개발/인프라/리팩토링 기술 어시스턴트입니다.
                응답은 우선순위와 단계 순서가 보이도록 작성하세요.
                최근 대화의 연속성과 이전 구조 결정을 반영하세요.
                필요하면 선택지보다 권장안을 먼저 제시하세요.
                """,
                parsedSummary,
                state,
                ragContext.getPromptBlock(),
                token -> sseEmitterHelper.send(emitter, SseEventNames.TOKEN, token)
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