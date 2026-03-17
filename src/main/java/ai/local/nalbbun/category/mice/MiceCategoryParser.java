package ai.local.nalbbun.category.mice;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.category.mice.parser.LlmMiceParser;
import ai.local.nalbbun.category.mice.parser.RuleBasedMiceParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * MiceCategoryParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: mice category parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class MiceCategoryParser
        extends AbstractHybridCategoryParser<MiceContext>
        implements CategoryParser<MiceContext> {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleBasedMiceParser ruleBasedMiceParser 값
     * @param llmMiceParser llmMiceParser 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    public MiceCategoryParser(
            RuleBasedMiceParser ruleBasedMiceParser,
            LlmMiceParser llmMiceParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedMiceParser, llmMiceParser, debugRuntimeConfigService);
    }

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @return MiceContext 타입의 처리 결과
     */
    @Override
    public MiceContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * newContext 기능을 수행한다.
     * @return MiceContext 타입의 처리 결과
     */
    @Override
    protected MiceContext newContext() {
        return new MiceContext();
    }

    /**
     * needsLlmAssist 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    @Override
    protected boolean needsLlmAssist(ConversationState state, MiceContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getEventType() == null
                || context.getDeliverableType() == null
                || context.getTargetRegion() == null
                || q.contains("방향성")
                || q.contains("전략")
                || q.contains("메시지")
                || q.contains("브랜딩");
    }

    /**
     * applyDefaults 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param state 현재 처리 상태 정보
     */
    @Override
    protected void applyDefaults(MiceContext context, ConversationState state) {
        if (context.getEventType() == null || context.getEventType().isBlank()) {
            context.setEventType("mice-event");
        }
        if (context.getDeliverableType() == null || context.getDeliverableType().isBlank()) {
            context.setDeliverableType("strategy");
        }
        if (context.getTargetRegion() == null || context.getTargetRegion().isBlank()) {
            context.setTargetRegion("global");
        }
    }

    /**
     * markMode 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param mode mode 값
     */
    @Override
    protected void markMode(MiceContext context, String mode) {
        context.setParserMode(mode);
    }
}