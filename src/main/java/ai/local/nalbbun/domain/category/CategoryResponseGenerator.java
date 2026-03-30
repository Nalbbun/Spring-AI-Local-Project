package ai.local.nalbbun.domain.category;

import java.util.function.Consumer;

import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.prompt.PromptMemoryContext;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelChatService;
import ai.local.nalbbun.domain.prompt.PromptMemoryContextBuilder;
import org.springframework.stereotype.Component;

/**
 * Category Response Generator 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class CategoryResponseGenerator {

    private final PromptMemoryContextBuilder promptMemoryContextBuilder;
    private final RuntimeModelChatService runtimeModelChatService;

    /**
     * Category Response Generator 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String generate(ChatCategory category,
                           String systemPrompt,
                           String parsedSummary,
                           ConversationState state,
                           String ragPromptBlock) {
        return runtimeModelChatService.callText(
                mapTarget(category),
                systemPrompt,
                buildUserPrompt(category, systemPrompt, parsedSummary, state, ragPromptBlock)
        );
    }

    /**
     * 토큰 단위 스트리밍 생성.
     * tokenConsumer 에 토큰을 즉시 전달하고, 완성된 전체 응답을 반환합니다.
     */
    public String generateStreaming(ChatCategory category,
                                    String systemPrompt,
                                    String parsedSummary,
                                    ConversationState state,
                                    String ragPromptBlock,
                                    Consumer<String> tokenConsumer) {
        return runtimeModelChatService.streamText(
                mapTarget(category),
                systemPrompt,
                buildUserPrompt(category, systemPrompt, parsedSummary, state, ragPromptBlock),
                tokenConsumer
        );
    }

    /**
     * generate Streaming 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String generateStreaming(ChatCategory category,
                                    String systemPrompt,
                                    String parsedSummary,
                                    ConversationState state,
                                    Consumer<String> tokenConsumer) {
        return generateStreaming(category, systemPrompt, parsedSummary, state, null, tokenConsumer);
    }

    /**
     * describe Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describeModel(ChatCategory category) {
        return runtimeModelChatService.describeResolvedModel(mapTarget(category), false);
    }

    /**
     * build User Prompt 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String buildUserPrompt(ChatCategory category,
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

        String userPrompt = promptTemplate.toUserPrompt();

        if (ragPromptBlock != null && !ragPromptBlock.isBlank()) {
            userPrompt = userPrompt + "\n\n" + ragPromptBlock;
        }
        return userPrompt;
    }

    /**
     * map Target 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RuntimeModelTarget mapTarget(ChatCategory category) {
        return switch (category) {
            case GENERAL -> RuntimeModelTarget.GENERAL;
            case DEV     -> RuntimeModelTarget.DEV;
            case MICE    -> RuntimeModelTarget.MICE;
            case TRAVEL  -> RuntimeModelTarget.TRAVEL_PLAN;
        };
    }
}
