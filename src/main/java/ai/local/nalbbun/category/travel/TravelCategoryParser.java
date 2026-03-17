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
 * Travel Category Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelCategoryParser
        extends AbstractHybridCategoryParser<TravelContext>
        implements CategoryParser<TravelContext> {

    /**
     * Travel Category Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public TravelContext parse(ConversationState state) {
        return super.parse(state);
    }

    /**
     * new Context 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    protected TravelContext newContext() {
        return new TravelContext();
    }

    /**
     * needs Llm Assist 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * apply Defaults 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * mark Mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    protected void markMode(TravelContext context, String mode) {
        context.setParserMode(mode);
    }
}