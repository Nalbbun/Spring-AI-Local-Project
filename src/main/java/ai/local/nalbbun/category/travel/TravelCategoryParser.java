package ai.local.nalbbun.category.travel;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.category.travel.parser.LlmTravelParser;
import ai.local.nalbbun.category.travel.parser.RuleBasedTravelParser;
import ai.local.nalbbun.internal.service.DebugRuntimeConfigService;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.ConversationState;
import org.springframework.stereotype.Component;

/**
 * 여행 카테고리 파서.
 *
 * 수정 이력:
 * - applyDefaults 에서 destination 기본값 "제주도" 하드코딩 제거
 *   → 목적지가 불명확하면 userQuery 원문을 그대로 사용
 * - needsLlmAssist 조건 강화
 *   → destination null 또는 RULE 파서 미매칭 시 LLM 보조 트리거
 */
@Component
public class TravelCategoryParser
        extends AbstractHybridCategoryParser<TravelContext>
        implements CategoryParser<TravelContext> {

    public TravelCategoryParser(
            RuleBasedTravelParser ruleBasedTravelParser,
            LlmTravelParser llmTravelParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedTravelParser, llmTravelParser, debugRuntimeConfigService);
    }

    @Override
    public ChatCategory category() { return ChatCategory.TRAVEL; }

    @Override
    public TravelContext parse(ConversationState state) { return super.parse(state); }

    @Override
    protected TravelContext newContext() { return new TravelContext(); }

    /**
     * LLM 보조가 필요한 경우:
     * 1. 목적지가 null (RULE 파서 미매칭)
     * 2. 일수가 null
     * 3. 예산이 null
     * 4. 모호한 표현 포함
     */
    @Override
    protected boolean needsLlmAssist(ConversationState state, TravelContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        // 목적지 미매칭 → 반드시 LLM 처리 (핵심 수정)
        if (context.getDestination() == null || context.getDestination().isBlank()) return true;
        // 일수 / 예산 미파싱
        if (context.getDays() == null || context.getDays() <= 0)      return true;
        if (context.getMaxBudget() == null || context.getMaxBudget() <= 0) return true;
        // 모호한 표현
        if (q.contains("적당히") || q.contains("알아서") || q.contains("무난하게")) return true;
        if (q.contains("커플") || q.contains("가족") || q.contains("부모님")
            || q.contains("아이와") || q.contains("혼자") || q.contains("친구"))  return true;

        return false;
    }

    /**
     * 기본값 적용.
     * destination 하드코딩("제주도") 제거 → 여전히 null이면 userQuery 원문 사용.
     * days / maxBudget 은 합리적 기본값 유지.
     */
    @Override
    protected void applyDefaults(TravelContext context, ConversationState state) {
        // ★ 목적지 기본값 하드코딩 제거
        // RULE, LLM 모두 추출 실패한 경우 → userQuery 원문을 목적지로 사용
        if (context.getDestination() == null || context.getDestination().isBlank()) {
            String q = state.getUserQuery();
            // 원문에서 "~여행", "~일정" 등 불필요한 접미사 제거
            String dest = q == null ? "여행지" : q
                .replaceAll("\\d+박\\d+일.*", "").replaceAll("\\d+일.*", "")
                .replaceAll("여행.*|일정.*|가이드.*|관광.*", "").trim();
            context.setDestination(dest.isBlank() ? "여행지" : dest);
        }
        // 일수 기본값: 2일
        if (context.getDays() == null || context.getDays() <= 0) {
            context.setDays(2);
        }
        // 예산 기본값: 50만원
        if (context.getMaxBudget() == null || context.getMaxBudget() <= 0) {
            context.setMaxBudget(500_000);
        }
    }

    @Override
    protected void markMode(TravelContext context, String mode) {
        context.setParserMode(mode);
    }
}
