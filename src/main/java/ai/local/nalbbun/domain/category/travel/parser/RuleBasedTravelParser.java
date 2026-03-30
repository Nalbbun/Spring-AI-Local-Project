package ai.local.nalbbun.domain.category.travel.parser;

import ai.local.nalbbun.domain.category.parser.CategoryParsingStrategy;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.category.model.ConversationState;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 규칙 기반 여행 파서.
 *
 * 핵심 원칙: 명확하게 매칭된 값만 설정, 불확실하면 null 반환.
 * → applyDefaults / LlmTravelParser 가 후처리로 보완.
 */
@Component
public class RuleBasedTravelParser implements CategoryParsingStrategy<TravelContext> {

    private static final Pattern NIGHTS_DAYS = Pattern.compile("(\\d+)박\\s*(\\d+)일");
    private static final Pattern DAYS_ONLY   = Pattern.compile("(\\d+)\\s*일");
    private static final Pattern MAN_WON     = Pattern.compile("(\\d+)\\s*만\\s*원");
    private static final Pattern WON         = Pattern.compile("(\\d{5,})\\s*원?");

    /**
     * 키워드 → 정규화된 목적지명 매핑.
     * 구체적 지명이 포함되면 해당 값 반환, 없으면 null (LLM 보조 트리거).
     */
    private static final Map<String, String> DESTINATION_MAP = new LinkedHashMap<>() {{
        // 광역시/도 단위
        put("서울",  "서울");
        put("부산",  "부산");
        put("대구",  "대구");
        put("인천",  "인천");
        put("광주",  "광주");
        put("대전",  "대전");
        put("울산",  "울산");
        put("세종",  "세종");
        // 도 단위 (앞 2자로 매칭 → "강원도", "강원" 모두 커버)
        put("강원",  "강원도");
        put("경기",  "경기도");
        put("충북",  "충청북도");
        put("충남",  "충청남도");
        put("충청",  "충청도");
        put("전북",  "전라북도");
        put("전남",  "전라남도");
        put("전라",  "전라도");
        put("경북",  "경상북도");
        put("경남",  "경상남도");
        put("경상",  "경상도");
        // 특별 지역
        put("제주",  "제주도");
        put("독도",  "독도");
        // 주요 도시/관광지
        put("강릉",  "강릉");
        put("속초",  "속초");
        put("춘천",  "춘천");
        put("원주",  "원주");
        put("평창",  "평창");
        put("여수",  "여수");
        put("순천",  "순천");
        put("전주",  "전주");
        put("군산",  "군산");
        put("경주",  "경주");
        put("안동",  "안동");
        put("통영",  "통영");
        put("거제",  "거제");
        put("포항",  "포항");
        put("수원",  "수원");
        put("용인",  "용인");
        put("가평",  "가평");
        put("양평",  "양평");
        put("남해",  "남해");
        put("목포",  "목포");
        put("구례",  "구례");
        put("하동",  "하동");
        put("담양",  "담양");
        put("보성",  "보성");
        put("태안",  "태안");
        put("보령",  "보령");
        put("공주",  "공주");
        put("부여",  "부여");
        put("서산",  "서산");
        // 해외 주요 목적지
        put("도쿄",  "도쿄");
        put("오사카", "오사카");
        put("교토",  "교토");
        put("후쿠오카", "후쿠오카");
        put("삿포로", "삿포로");
        put("방콕",  "방콕");
        put("파리",  "파리");
        put("뉴욕",  "뉴욕");
        put("런던",  "런던");
        put("베트남", "베트남");
        put("하노이", "하노이");
        put("호치민", "호치민");
        put("다낭",  "다낭");
        put("발리",  "발리");
        put("홍콩",  "홍콩");
        put("싱가포르", "싱가포르");
        put("대만",  "대만");
        put("타이베이", "타이베이");
    }};

    @Override
    public TravelContext parse(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery();
        context.setUserQuery(userQuery);

        if (context.getDestination() == null) {
            context.setDestination(extractDestination(userQuery));  // null 가능
        }
        if (context.getDays() == null) {
            Integer days = extractDays(userQuery);
            if (days != null) context.setDays(days);
        }
        if (context.getMaxBudget() == null) {
            Integer budget = extractBudget(userQuery);
            if (budget != null) context.setMaxBudget(budget);
        }

        return context;
    }

    @Override
    public String mode() { return "RULE"; }

    // ── 목적지 추출 ────────────────────────────────────────────────
    /**
     * 키워드 매칭으로 목적지 추출.
     * 매칭되지 않으면 null 반환 (하드코딩 기본값 제거).
     */
    private String extractDestination(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) return null;

        String q = userQuery.toLowerCase();
        for (Map.Entry<String, String> entry : DESTINATION_MAP.entrySet()) {
            if (q.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 매칭 없음 → null 반환 (LLM 파서가 처리)
        return null;
    }

    // ── 일수 추출 ───────────────────────────────────────────────────
    private Integer extractDays(String userQuery) {
        if (userQuery == null) return null;

        Matcher nightsDays = NIGHTS_DAYS.matcher(userQuery);
        if (nightsDays.find()) {
            return Integer.parseInt(nightsDays.group(2));
        }
        Matcher daysOnly = DAYS_ONLY.matcher(userQuery);
        if (daysOnly.find()) {
            int d = Integer.parseInt(daysOnly.group(1));
            // 숫자가 너무 크거나 작으면 무시 (예: "100일" 같은 오파싱 방지)
            if (d >= 1 && d <= 30) return d;
        }
        return null;  // 불명확 → null
    }

    // ── 예산 추출 ───────────────────────────────────────────────────
    private Integer extractBudget(String userQuery) {
        if (userQuery == null) return null;

        String cleaned = userQuery.replace(",", "");

        Matcher manWon = MAN_WON.matcher(cleaned);
        if (manWon.find()) {
            return Integer.parseInt(manWon.group(1)) * 10_000;
        }
        Matcher won = WON.matcher(cleaned);
        if (won.find()) {
            return Integer.parseInt(won.group(1));
        }
        return null;  // 불명확 → null
    }
}
