package ai.local.nalbbun.category.general;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.category.general.parser.LlmGeneralParser;
import ai.local.nalbbun.category.general.parser.RuleBasedGeneralParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * GeneralCategoryParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: general category parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class GeneralCategoryParser
        extends AbstractHybridCategoryParser<GeneralContext>
        implements CategoryParser<GeneralContext> {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleBasedGeneralParser ruleBasedGeneralParser 값
     * @param llmGeneralParser llmGeneralParser 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    public GeneralCategoryParser(
            RuleBasedGeneralParser ruleBasedGeneralParser,
            LlmGeneralParser llmGeneralParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedGeneralParser, llmGeneralParser, debugRuntimeConfigService);
    }

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @return GeneralContext 타입의 처리 결과
     */
    @Override
    public GeneralContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * newContext 기능을 수행한다.
     * @return GeneralContext 타입의 처리 결과
     */
    @Override
    protected GeneralContext newContext() {
        return new GeneralContext();
    }

    /**
     * needsLlmAssist 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    @Override
    protected boolean needsLlmAssist(ConversationState state, GeneralContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();
        return q.length() > 20 || q.contains("조금 더") || q.contains("다시") || q.contains("요약");
    }

    /**
     * applyDefaults 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param state 현재 처리 상태 정보
     */
    @Override
    protected void applyDefaults(GeneralContext context, ConversationState state) {
        if (context.getIntent() == null || context.getIntent().isBlank()) {
            context.setIntent("general_qa");
        }
    }

    /**
     * markMode 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param mode mode 값
     */
    @Override
    protected void markMode(GeneralContext context, String mode) {
        context.setParserMode(mode);
    }
}