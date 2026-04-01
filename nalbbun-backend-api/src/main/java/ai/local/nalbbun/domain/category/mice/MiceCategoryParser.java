package ai.local.nalbbun.domain.category.mice;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.category.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.domain.category.parser.CategoryParser;
import ai.local.nalbbun.domain.category.mice.model.MiceContext;
import ai.local.nalbbun.domain.category.mice.parser.LlmMiceParser;
import ai.local.nalbbun.domain.category.mice.parser.RuleBasedMiceParser;
import ai.local.nalbbun.domain.runtime.port.RuntimeCategoryPolicyPort;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;

/**
 * Mice Category Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class MiceCategoryParser
        extends AbstractHybridCategoryParser<MiceContext>
        implements CategoryParser<MiceContext> {

    /**
     * Mice Category Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public MiceCategoryParser(
            RuleBasedMiceParser ruleBasedMiceParser,
            LlmMiceParser llmMiceParser,
            RuntimeCategoryPolicyPort debugRuntimeConfigService
    ) {
        super(ruleBasedMiceParser, llmMiceParser, debugRuntimeConfigService);
    }

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public MiceContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * new Context 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    protected MiceContext newContext() {
        return new MiceContext();
    }

    /**
     * needs Llm Assist 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * apply Defaults 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * mark Mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    protected void markMode(MiceContext context, String mode) {
        context.setParserMode(mode);
    }
}