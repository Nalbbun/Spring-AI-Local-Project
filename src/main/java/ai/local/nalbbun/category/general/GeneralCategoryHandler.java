package ai.local.nalbbun.category.general;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.CategoryResponseGenerator;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.category.model.CategoryResult;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.ConversationState;
import ai.local.nalbbun.memory.service.ConversationMemoryService;
import ai.local.nalbbun.prompt.service.PromptService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import ai.local.nalbbun.support.sse.SseEmitterHelper;
import ai.local.nalbbun.support.sse.SseEventNames;
import lombok.RequiredArgsConstructor;

/**
 * General Category Handler 타입이다.
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
    private final PromptService promptService;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 일반 질의응답 어시스턴트입니다.
            최근 대화와 중요 메모를 참고하되, 현재 질문에 직접적으로 답변하세요.
            불필요하게 과거 내용을 반복하지 말고 자연스럽게 이어서 답변하세요.
            """;

    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    @Override
    public CategoryResult handle(ConversationState state, SseEmitter emitter) {
        agentEventPublisher.send(emitter, "GeneralCategoryHandler", "running", "GENERAL 질문 해석 중...");

        GeneralContext context = parser.parse(state);
        state.setCategoryContext(context);

        String parsedSummary = String.format("intent=%s", context.getIntent());

        // 등록된 프롬프트 우선, 없으면 내장 기본값 사용
        String resolvedPromptId = state.getPromptId();
        String systemPrompt = promptService.resolveSystemPrompt(resolvedPromptId, ChatCategory.GENERAL)
                .orElse(DEFAULT_SYSTEM_PROMPT);

        // 프롬프트 적용 정보 로그
        String promptLabel = resolvedPromptId != null && !resolvedPromptId.isBlank()
                ? "선택 프롬프트(id=" + resolvedPromptId + ")"
                : (promptService.resolveSystemPrompt(null, ChatCategory.GENERAL).isPresent()
                        ? "DB 기본 프롬프트" : "내장 기본 프롬프트");
        agentEventPublisher.send(emitter, "PromptTrace-GENERAL", "info",
                "prompt=" + promptLabel + " | preview=" + systemPrompt.trim().replace("\n", " ").substring(0, Math.min(60, systemPrompt.trim().length())) + "...");

        agentEventPublisher.send(emitter, "ModelTrace-GENERAL", "info",
                "response=" + responseGenerator.describeModel(ChatCategory.GENERAL));

        String response = responseGenerator.generateStreaming(
                ChatCategory.GENERAL, systemPrompt, parsedSummary, state,
                token -> sseEmitterHelper.send(emitter, SseEventNames.TOKEN, token));

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.GENERAL, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(emitter, "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage());

        agentEventPublisher.send(emitter, "GeneralCategoryHandler", "complete", "GENERAL 응답 생성 완료");

        return CategoryResult.builder().finalResponse(response).payload(context).build();
    }
}