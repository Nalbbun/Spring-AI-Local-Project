package ai.local.nalbbun.category.dev;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.category.dev.parser.LlmDevParser;
import ai.local.nalbbun.category.dev.parser.RuleBasedDevParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * DevCategoryParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: dev category parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class DevCategoryParser
        extends AbstractHybridCategoryParser<DevContext>
        implements CategoryParser<DevContext> {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleBasedDevParser ruleBasedDevParser 값
     * @param llmDevParser llmDevParser 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    public DevCategoryParser(
            RuleBasedDevParser ruleBasedDevParser,
            LlmDevParser llmDevParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedDevParser, llmDevParser, debugRuntimeConfigService);
    }

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @return DevContext 타입의 처리 결과
     */
    @Override
    public DevContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * newContext 기능을 수행한다.
     * @return DevContext 타입의 처리 결과
     */
    @Override
    protected DevContext newContext() {
        return new DevContext();
    }

    /**
     * needsLlmAssist 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    @Override
    protected boolean needsLlmAssist(ConversationState state, DevContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getTaskType() == null
                || context.getTopic() == null
                || q.contains("전반적으로")
                || q.contains("정리해줘")
                || q.contains("어떻게 할까")
                || q.contains("설계부터")
                || q.contains("구현 방향");
    }

    /**
     * applyDefaults 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param state 현재 처리 상태 정보
     */
    @Override
    protected void applyDefaults(DevContext context, ConversationState state) {
        if (context.getTaskType() == null || context.getTaskType().isBlank()) {
            context.setTaskType("implementation");
        }
        if (context.getTopic() == null || context.getTopic().isBlank()) {
            context.setTopic("general-dev");
        }
    }

    /**
     * markMode 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param mode mode 값
     */
    @Override
    protected void markMode(DevContext context, String mode) {
        context.setParserMode(mode);
    }
}