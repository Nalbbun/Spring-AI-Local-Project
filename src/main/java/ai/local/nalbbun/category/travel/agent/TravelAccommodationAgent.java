package ai.local.nalbbun.category.travel.agent;

import ai.local.nalbbun.category.travel.model.Accommodation;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.port.WebSearchPort;
import ai.local.nalbbun.service.llm.RuntimeModelChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TravelAccommodationAgent는 세부 업무를 분리하여 수행하는 에이전트이다.
 * <p>주요 기능: travel accommodation agent 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelAccommodationAgent {

    /** DEFAULT_PRICE_PER_NIGHT 값을 보관한다. */
    private static final Map<String, Integer> DEFAULT_PRICE_PER_NIGHT = Map.of(
            "호텔", 150000,
            "리조트", 180000,
            "펜션", 100000,
            "게스트하우스", 60000,
            "숙소", 120000
    );

    /** SYSTEM_PROMPT 값을 보관한다. */
    private static final String SYSTEM_PROMPT = """
        당신은 숙소 추천 전문 에이전트입니다.
        도구를 활용해 사용자의 요청에 맞는 숙소 후보를 추천하세요.
        반드시 JSON 배열만 출력하세요.
        """;

    /** USER_PROMPT_TEMPLATE 값을 보관한다. */
    private static final String USER_PROMPT_TEMPLATE = """
        사용자 요청: %s
        - 호텔/리조트/펜션/게스트하우스 등 다양한 유형을 섞어 추천하세요.
        - 각 항목에는 name, address, description, pricePerNight를 포함하세요.
        - 반드시 JSON 배열만 출력하세요.
        """;

    /** runtimeModelChatService 값을 보관한다. */
    private final RuntimeModelChatService runtimeModelChatService;
    /** webSearchPort 값을 보관한다. */
    private final WebSearchPort webSearchPort;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param runtimeModelChatService runtimeModelChatService 값
     * @param webSearchPort webSearchPort 값
     */
    public TravelAccommodationAgent(
            RuntimeModelChatService runtimeModelChatService,
            WebSearchPort webSearchPort
    ) {
        this.runtimeModelChatService = runtimeModelChatService;
        this.webSearchPort = webSearchPort;
    }

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return 조회 또는 생성된 목록
     */
    public List<Accommodation> execute(String userQuery) {
        String userMessage = String.format(Locale.KOREAN, USER_PROMPT_TEMPLATE, userQuery);
        List<Accommodation> result = callAsEntity(userMessage);
        return normalize(result);
    }

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     */
    public void execute(TravelContext context) {
        String query;
        if (context.isReplan()) {
            query = String.format("%s 가성비 저렴한 숙소 추천", context.getDestination());
        } else {
            query = String.format("%s 숙소 추천", context.getDestination());
        }
        context.setAccommodations(execute(query));
    }

    /**
     * callAsEntity 기능을 수행한다.
     *
     * @param userMessage userMessage 값
     * @return 조회 또는 생성된 목록
     */
    private List<Accommodation> callAsEntity(String userMessage) {
        try {
            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage,
                    this,
                    new ParameterizedTypeReference<List<Accommodation>>() {}
            );
        } catch (Exception first) {
            String repairMessage = """
                이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
                반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
                JSON 스키마: [{"name":"...","address":"...","description":"...","pricePerNight":12345}]
                """;

            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage + "\n\n" + repairMessage,
                    this,
                    new ParameterizedTypeReference<List<Accommodation>>() {}
            );
        }
    }

    /**
     * normalize 기능을 수행한다.
     *
     * @param items items 목록 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Accommodation> normalize(List<Accommodation> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Accommodation> normalized = new ArrayList<>(items.size());
        for (Accommodation accommodation : items) {
            if (accommodation == null || accommodation.getName() == null || accommodation.getName().isBlank()) {
                continue;
            }

            Integer price = accommodation.getPricePerNight();
            if (price == null || price <= 0) {
                int fallback = inferDefaultPrice(accommodation);

                String desc = accommodation.getDescription();
                String patchedDesc = (desc == null ? "" : desc);
                if (!patchedDesc.contains("기본요금")) {
                    patchedDesc = patchedDesc.isBlank()
                            ? String.format(Locale.KOREAN, "기본요금 적용(%d원)", fallback)
                            : String.format(Locale.KOREAN, "%s (기본요금 적용: %d원)", patchedDesc, fallback);
                }

                accommodation.setPricePerNight(fallback);
                accommodation.setDescription(patchedDesc);
            }
            normalized.add(accommodation);
        }
        return normalized;
    }

    /**
     * inferDefaultPrice 기능을 수행한다.
     *
     * @param a a 값
     * @return int 타입의 처리 결과
     */
    private int inferDefaultPrice(Accommodation a) {
        String name = safe(a.getName());
        String desc = safe(a.getDescription());
        String text = (name + " " + desc).trim();

        if (text.contains("리조트")) {
            return DEFAULT_PRICE_PER_NIGHT.get("리조트");
        }
        if (text.contains("게스트하우스") || text.contains("호스텔")) {
            return DEFAULT_PRICE_PER_NIGHT.get("게스트하우스");
        }
        if (text.contains("펜션")) {
            return DEFAULT_PRICE_PER_NIGHT.get("펜션");
        }
        if (text.contains("호텔")) {
            return DEFAULT_PRICE_PER_NIGHT.get("호텔");
        }
        return DEFAULT_PRICE_PER_NIGHT.get("숙소");
    }

    /**
     * safe 기능을 수행한다.
     *
     * @param s s 값
     * @return 처리 결과 문자열
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }
    
    /**
     * describeModel 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String describeModel() {
        return runtimeModelChatService.describeResolvedModel(RuntimeModelTarget.TRAVEL_SEARCH, true);
    }
    
    /**
     * 대상 정보를 조회한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    @Tool(description = "숙소 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
    public String searchAccommodations(@ToolParam(description = "검색 쿼리") String query) {
        return webSearchPort.search(query);
    }

    /**
     * fetchAccommodationInfo 기능을 수행한다.
     *
     * @param url 대상 URL
     * @return 처리 결과 문자열
     */
    @Tool(description = "웹 페이지의 본문 텍스트를 가져와 숙소 상세 정보를 제공합니다.")
    public String fetchAccommodationInfo(@ToolParam(description = "웹 페이지 URL") String url) {
        return webSearchPort.fetch(url);
    }
}