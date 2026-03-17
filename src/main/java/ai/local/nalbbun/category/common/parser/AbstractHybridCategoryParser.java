package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.internal.service.DebugRuntimeConfigService;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.CategoryContext;
import ai.local.nalbbun.category.model.ConversationState;

/**
 * Abstract Hybrid Category Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public abstract class AbstractHybridCategoryParser<T extends CategoryContext> {

    private final CategoryParsingStrategy<T> ruleStrategy;
    private final CategoryParsingStrategy<T> llmStrategy;
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    /**
     * Abstract Hybrid Category Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    protected AbstractHybridCategoryParser(
            CategoryParsingStrategy<T> ruleStrategy,
            CategoryParsingStrategy<T> llmStrategy,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.ruleStrategy = ruleStrategy;
        this.llmStrategy = llmStrategy;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public T parse(ConversationState state) {
        T context = newContext();
        CategoryParserMode mode = debugRuntimeConfigService.getParserMode(category());

        switch (mode) {
            case RULE -> {
                context = ruleStrategy.parse(state, context);
                applyDefaults(context, state);
                markMode(context, "RULE");
                return context;
            }
            case LLM -> {
                context = llmStrategy.parse(state, context);
                applyDefaults(context, state);
                markMode(context, "LLM");
                return context;
            }
            case HYBRID -> {
                context = ruleStrategy.parse(state, context);

                if (needsLlmAssist(state, context)) {
                    context = llmStrategy.parse(state, context);
                    applyDefaults(context, state);
                    markMode(context, "HYBRID(RULE->LLM)");
                } else {
                    applyDefaults(context, state);
                    markMode(context, "HYBRID(RULE)");
                }
                return context;
            }
            default -> throw new IllegalStateException("Unsupported parser mode: " + mode);
        }
    }

    protected abstract ChatCategory category();

    protected abstract T newContext();

    protected abstract boolean needsLlmAssist(ConversationState state, T context);

    protected abstract void applyDefaults(T context, ConversationState state);

    protected abstract void markMode(T context, String mode);
}