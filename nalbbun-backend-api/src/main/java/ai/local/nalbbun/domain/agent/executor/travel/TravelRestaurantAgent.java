package ai.local.nalbbun.domain.agent.executor.travel;

import ai.local.nalbbun.domain.category.travel.model.Restaurant;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.domain.search.port.WebSearchPort;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Travel Restaurant Agent 타입이다.
 *
 * <p>기능 설명: 특정 작업 목적에 맞는 세부 처리 단위를 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
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

    /**
     * Travel Restaurant Agent 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public TravelRestaurantAgent(
            RuntimeModelChatService runtimeModelChatService,
            WebSearchPort webSearchPort
    ) {
        this.runtimeModelChatService = runtimeModelChatService;
        this.webSearchPort = webSearchPort;
    }

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<Restaurant> execute(String userQuery) {
        String userMessage = String.format(USER_PROMPT_TEMPLATE, userQuery);
        List<Restaurant> result = callAsEntity(userMessage);
        return normalize(deduplicateByName(result));
    }

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void execute(TravelContext context) {
        String query;
        if (context.isReplan()) {
            query = String.format("%s 가성비 저렴한 맛집 추천", context.getDestination());
        } else {
            query = String.format("%s 맛집 추천", context.getDestination());
        }
        context.setRestaurants(execute(query));
    }

    /**
     * call As Entity 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * deduplicate By Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * normalize 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * infer Default Price 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * contains Any 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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
    /**
     * describe Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describeModel() {
        return runtimeModelChatService.describeResolvedModel(RuntimeModelTarget.TRAVEL_SEARCH, true);
    }
    
    /**
     * search Restaurants 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Tool(description = "맛집 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
    public String searchRestaurants(@ToolParam(description = "검색 쿼리") String query) {
        return webSearchPort.search(query);
    }

    /**
     * fetch Restaurant Info 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Tool(description = "웹 페이지의 본문 텍스트를 가져와 맛집 상세 정보를 제공합니다.")
    public String fetchRestaurantInfo(@ToolParam(description = "웹 페이지 URL") String url) {
        return webSearchPort.fetch(url);
    }
}