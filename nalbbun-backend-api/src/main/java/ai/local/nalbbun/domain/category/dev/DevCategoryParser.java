package ai.local.nalbbun.domain.category.dev;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.category.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.domain.category.parser.CategoryParser;
import ai.local.nalbbun.domain.category.dev.model.DevContext;
import ai.local.nalbbun.domain.category.dev.parser.LlmDevParser;
import ai.local.nalbbun.domain.category.dev.parser.RuleBasedDevParser;
import ai.local.nalbbun.admin.service.DebugRuntimeConfigService;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;

/**
 * Dev Category Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class DevCategoryParser
        extends AbstractHybridCategoryParser<DevContext>
        implements CategoryParser<DevContext> {

    /**
     * Dev Category Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public DevContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * new Context 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    protected DevContext newContext() {
        return new DevContext();
    }

    /**
     * needs Llm Assist 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * apply Defaults 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * mark Mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    protected void markMode(DevContext context, String mode) {
        context.setParserMode(mode);
    }
}