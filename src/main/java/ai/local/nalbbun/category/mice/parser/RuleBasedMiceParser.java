package ai.local.nalbbun.category.mice.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedMiceParser implements CategoryParsingStrategy<MiceContext> {

    @Override
    public MiceContext parse(ConversationState state, MiceContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        context.setEventType(inferEventType(q));
        context.setDeliverableType(inferDeliverable(q));
        context.setTargetRegion(inferRegion(q));
        return context;
    }

    @Override
    public String mode() {
        return "RULE";
    }

    private String inferEventType(String q) {
        if (containsAny(q, "포럼")) return "forum";
        if (containsAny(q, "컨퍼런스", "conference")) return "conference";
        if (containsAny(q, "전시", "박람회", "expo")) return "exhibition";
        if (containsAny(q, "공연", "페스티벌")) return "festival";
        return "mice-event";
    }

    private String inferDeliverable(String q) {
        if (containsAny(q, "제안서", "기획안")) return "proposal";
        if (containsAny(q, "운영안", "운영")) return "operations";
        if (containsAny(q, "프로그램", "세션")) return "program";
        if (containsAny(q, "슬로건", "메시지", "브랜딩")) return "branding";
        return "strategy";
    }

    private String inferRegion(String q) {
        if (containsAny(q, "태국", "방콕")) return "thailand";
        if (containsAny(q, "베트남", "호치민", "하노이")) return "vietnam";
        if (containsAny(q, "말레이시아", "쿠알라룸푸르")) return "malaysia";
        if (containsAny(q, "부탄")) return "bhutan";
        if (containsAny(q, "한국", "서울", "부산", "제주")) return "korea";
        return "global";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}