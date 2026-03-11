package ai.local.nalbbun.category.travel.agent;

import ai.local.nalbbun.category.travel.model.Restaurant;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.port.WebSearchPort;
import ai.local.nalbbun.service.llm.RuntimeModelChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TravelRestaurantAgent {

    private static final Map<String, Integer> DEFAULT_PRICE = Map.of(
            "한식", 12000,
            "일식/초밥", 18000,
            "중식", 15000,
            "양식/스테이크", 25000,
            "해산물", 20000,
            "분식/간식", 8000,
            "카페/디저트", 10000,
            "기타", 15000
    );

    private static final String SYSTEM_PROMPT = """
        당신은 맛집 추천 전문 에이전트입니다.
        도구를 활용해 사용자 요청에 맞는 맛집 후보를 추천하세요.
        반드시 JSON 배열만 출력하세요.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        사용자 요청: %s
        - 지역과 음식 종류를 고려해 다양한 맛집을 추천하세요.
        - 각 항목에는 name, address, description, price를 포함하세요.
        - 반드시 JSON 배열만 출력하세요.
        """;

    private final RuntimeModelChatService runtimeModelChatService;
    private final WebSearchPort webSearchPort;

    public TravelRestaurantAgent(
            RuntimeModelChatService runtimeModelChatService,
            WebSearchPort webSearchPort
    ) {
        this.runtimeModelChatService = runtimeModelChatService;
        this.webSearchPort = webSearchPort;
    }

    public List<Restaurant> execute(String userQuery) {
        String userMessage = String.format(USER_PROMPT_TEMPLATE, userQuery);
        List<Restaurant> result = callAsEntity(userMessage);
        return normalize(deduplicateByName(result));
    }

    public void execute(TravelContext context) {
        String query;
        if (context.isReplan()) {
            query = String.format("%s 가성비 저렴한 맛집 추천", context.getDestination());
        } else {
            query = String.format("%s 맛집 추천", context.getDestination());
        }
        context.setRestaurants(execute(query));
    }

    private List<Restaurant> callAsEntity(String userMessage) {
        try {
            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage,
                    this,
                    new ParameterizedTypeReference<List<Restaurant>>() {}
            );
        } catch (Exception first) {
            String repairMessage = """
                이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
                반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
                JSON 스키마: [{"name":"...","address":"...","description":"...","price":12345}]
                """;

            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage + "\n\n" + repairMessage,
                    this,
                    new ParameterizedTypeReference<List<Restaurant>>() {}
            );
        }
    }

    private List<Restaurant> deduplicateByName(List<Restaurant> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, Restaurant> byName = new LinkedHashMap<>();
        for (Restaurant r : items) {
            if (r == null || r.getName() == null || r.getName().isBlank()) {
                continue;
            }
            byName.putIfAbsent(r.getName().trim(), r);
        }
        return new ArrayList<>(byName.values());
    }

    private List<Restaurant> normalize(List<Restaurant> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Restaurant> normalized = new ArrayList<>(items.size());
        for (Restaurant r : items) {
            if (r == null) {
                continue;
            }

            Integer price = r.getPrice();
            if (price == null || price <= 0) {
                int fallbackPrice = inferDefaultPrice(r);

                String desc = r.getDescription();
                String patchedDesc = (desc == null ? "" : desc);
                if (!patchedDesc.contains("기본가격") && !patchedDesc.contains("기본 가격")) {
                    patchedDesc = patchedDesc.isBlank()
                            ? String.format(Locale.KOREAN, "기본 가격 적용(%d원)", fallbackPrice)
                            : String.format(Locale.KOREAN, "%s (기본 가격 적용: %d원)", patchedDesc, fallbackPrice);
                }

                r.setPrice(fallbackPrice);
                r.setDescription(patchedDesc);
            }
            normalized.add(r);
        }
        return normalized;
    }

    private int inferDefaultPrice(Restaurant r) {
        String name = r.getName() == null ? "" : r.getName();
        String desc = r.getDescription() == null ? "" : r.getDescription();
        String text = (name + " " + desc).trim();

        if (containsAny(text, "한식", "백반", "국밥", "국수", "칼국수", "갈비", "삼겹", "김치", "냉면", "비빔밥")) {
            return DEFAULT_PRICE.get("한식");
        }
        if (containsAny(text, "일식", "초밥", "스시", "사시미", "라멘", "돈카츠", "우동")) {
            return DEFAULT_PRICE.get("일식/초밥");
        }
        if (containsAny(text, "중식", "짜장", "짬뽕", "탕수육", "마라", "훠궈")) {
            return DEFAULT_PRICE.get("중식");
        }
        if (containsAny(text, "양식", "스테이크", "파스타", "피자", "버거", "브런치")) {
            return DEFAULT_PRICE.get("양식/스테이크");
        }
        if (containsAny(text, "해산물", "회", "조개", "대게", "랍스터", "굴", "해물")) {
            return DEFAULT_PRICE.get("해산물");
        }
        if (containsAny(text, "분식", "떡볶이", "김밥", "튀김", "순대", "라볶이")) {
            return DEFAULT_PRICE.get("분식/간식");
        }
        if (containsAny(text, "카페", "커피", "디저트", "베이커리", "케이크", "빵")) {
            return DEFAULT_PRICE.get("카페/디저트");
        }
        return DEFAULT_PRICE.get("기타");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }
    public String describeModel() {
        return runtimeModelChatService.describeResolvedModel(RuntimeModelTarget.TRAVEL_SEARCH, true);
    }
    
    @Tool(description = "맛집 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
    public String searchRestaurants(@ToolParam(description = "검색 쿼리") String query) {
        return webSearchPort.search(query);
    }

    @Tool(description = "웹 페이지의 본문 텍스트를 가져와 맛집 상세 정보를 제공합니다.")
    public String fetchRestaurantInfo(@ToolParam(description = "웹 페이지 URL") String url) {
        return webSearchPort.fetch(url);
    }
}