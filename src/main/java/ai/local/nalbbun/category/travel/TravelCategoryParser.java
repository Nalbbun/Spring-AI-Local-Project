package ai.local.nalbbun.category.travel;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.category.travel.parser.LlmTravelParser;
import ai.local.nalbbun.category.travel.parser.RuleBasedTravelParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * TravelCategoryParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: travel category parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelCategoryParser
        extends AbstractHybridCategoryParser<TravelContext>
        implements CategoryParser<TravelContext> {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleBasedTravelParser ruleBasedTravelParser 값
     * @param llmTravelParser llmTravelParser 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    public TravelCategoryParser(
            RuleBasedTravelParser ruleBasedTravelParser,
            LlmTravelParser llmTravelParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedTravelParser, llmTravelParser, debugRuntimeConfigService);
    }

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @return TravelContext 타입의 처리 결과
     */
    @Override
    public TravelContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * newContext 기능을 수행한다.
     * @return TravelContext 타입의 처리 결과
     */
    @Override
    protected TravelContext newContext() {
        return new TravelContext();
    }

    /**
     * needsLlmAssist 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    @Override
    protected boolean needsLlmAssist(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getDestination() == null
                || context.getDays() == null
                || context.getMaxBudget() == null
                || userQuery.contains("적당히")
                || userQuery.contains("알아서")
                || userQuery.contains("무난하게")
                || userQuery.contains("커플")
                || userQuery.contains("가족")
                || userQuery.contains("부모님")
                || userQuery.contains("아이와");
    }

    /**
     * applyDefaults 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param state 현재 처리 상태 정보
     */
    @Override
    protected void applyDefaults(TravelContext context, ConversationState state) {
        if (context.getDestination() == null || context.getDestination().isBlank()) {
            context.setDestination("제주도");
        }
        if (context.getDays() == null || context.getDays() <= 0) {
            context.setDays(2);
        }
        if (context.getMaxBudget() == null || context.getMaxBudget() <= 0) {
            context.setMaxBudget(500000);
        }
    }

    /**
     * markMode 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param mode mode 값
     */
    @Override
    protected void markMode(TravelContext context, String mode) {
        context.setParserMode(mode);
    }
}