package ai.local.nalbbun.category.common;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.model.prompt.PromptMemoryContext;
import ai.local.nalbbun.service.llm.RuntimeModelChatService;
import ai.local.nalbbun.service.prompt.PromptMemoryContextBuilder;
import org.springframework.stereotype.Component;

@Component
public class CategoryResponseGenerator {

    private final PromptMemoryContextBuilder promptMemoryContextBuilder;
    private final RuntimeModelChatService runtimeModelChatService;

    public CategoryResponseGenerator(
            PromptMemoryContextBuilder promptMemoryContextBuilder,
            RuntimeModelChatService runtimeModelChatService
    ) {
        this.promptMemoryContextBuilder = promptMemoryContextBuilder;
        this.runtimeModelChatService = runtimeModelChatService;
    }

    public String generate(ChatCategory category,
                           String systemPrompt,
                           String parsedSummary,
                           ConversationState state) {

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

        return runtimeModelChatService.callText(
                target,
                systemPrompt,
                promptTemplate.toUserPrompt()
        );
    }

    public String describeModel(ChatCategory category) {
        return runtimeModelChatService.describeResolvedModel(mapTarget(category), false);
    }

    private RuntimeModelTarget mapTarget(ChatCategory category) {
        return switch (category) {
            case GENERAL -> RuntimeModelTarget.GENERAL;
            case DEV -> RuntimeModelTarget.DEV;
            case MICE -> RuntimeModelTarget.MICE;
            case TRAVEL -> RuntimeModelTarget.TRAVEL_PLAN;
        };
    }
}