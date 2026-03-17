package ai.local.nalbbun.category.common;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.model.prompt.PromptMemoryContext;
import ai.local.nalbbun.service.llm.RuntimeModelChatService;
import ai.local.nalbbun.service.prompt.PromptMemoryContextBuilder;
import org.springframework.stereotype.Component;

/**
 * CategoryResponseGenerator는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: category response generator 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class CategoryResponseGenerator {

    /** promptMemoryContextBuilder 값을 보관한다. */
    private final PromptMemoryContextBuilder promptMemoryContextBuilder;
    /** runtimeModelChatService 값을 보관한다. */
    private final RuntimeModelChatService runtimeModelChatService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param promptMemoryContextBuilder promptMemoryContextBuilder 값
     * @param runtimeModelChatService runtimeModelChatService 값
     */
    public CategoryResponseGenerator(
            PromptMemoryContextBuilder promptMemoryContextBuilder,
            RuntimeModelChatService runtimeModelChatService
    ) {
        this.promptMemoryContextBuilder = promptMemoryContextBuilder;
        this.runtimeModelChatService = runtimeModelChatService;
    }

    /**
     * generate 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param systemPrompt systemPrompt 값
     * @param parsedSummary parsedSummary 값
     * @param state 현재 처리 상태 정보
     * @return 처리 결과 문자열
     */
    public String generate(ChatCategory category,
                           String systemPrompt,
                           String parsedSummary,
                           ConversationState state) {
        return generate(category, systemPrompt, parsedSummary, state, null);
    }

    /**
     * generate 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param systemPrompt systemPrompt 값
     * @param parsedSummary parsedSummary 값
     * @param state 현재 처리 상태 정보
     * @param ragPromptBlock ragPromptBlock 값
     * @return 처리 결과 문자열
     */
    public String generate(ChatCategory category,
                           String systemPrompt,
                           String parsedSummary,
                           ConversationState state,
                           String ragPromptBlock) {

        PromptMemoryContext memoryContext = promptMemoryContextBuilder.build(
                state.getConversationId(),
                category
        );

        CategoryPromptTemplate promptTemplate = CategoryPromptTemplate.builder()
                .systemPrompt(systemPrompt)
                .parsedSummary(parsedSummary)
                .categorySummary(memoryContext.getCategorySummary())
                .importantNotes(memoryContext.getImportantNotesBlock())
                .recentConversation(memoryContext.getRecentConversationBlock())
                .currentUserQuery(state.getUserQuery())
                .build();

        RuntimeModelTarget target = mapTarget(category);
        String userPrompt = promptTemplate.toUserPrompt();

        if (ragPromptBlock != null && !ragPromptBlock.isBlank()) {
            userPrompt = userPrompt + "\n\n" + ragPromptBlock;
        }

        return runtimeModelChatService.callText(
                target,
                systemPrompt,
                userPrompt
        );
    }

    /**
     * describeModel 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @return 처리 결과 문자열
     */
    public String describeModel(ChatCategory category) {
        return runtimeModelChatService.describeResolvedModel(mapTarget(category), false);
    }

    /**
     * mapTarget 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @return RuntimeModelTarget 타입의 처리 결과
     */
    private RuntimeModelTarget mapTarget(ChatCategory category) {
        return switch (category) {
            case GENERAL -> RuntimeModelTarget.GENERAL;
            case DEV -> RuntimeModelTarget.DEV;
            case MICE -> RuntimeModelTarget.MICE;
            case TRAVEL -> RuntimeModelTarget.TRAVEL_PLAN;
        };
    }
}
