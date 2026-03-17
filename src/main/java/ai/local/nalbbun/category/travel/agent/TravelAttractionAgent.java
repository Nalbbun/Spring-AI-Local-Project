package ai.local.nalbbun.category.travel.agent;

import ai.local.nalbbun.category.travel.model.Attraction;
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
 * TravelAttractionAgent는 세부 업무를 분리하여 수행하는 에이전트이다.
 * <p>주요 기능: travel attraction agent 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelAttractionAgent {

    /** DEFAULT_ENTRANCE_FEE 값을 보관한다. */
    private static final Map<String, Integer> DEFAULT_ENTRANCE_FEE = Map.of(
            "국립공원/자연명소", 0,
            "박물관/미술관", 5000,
            "테마파크/놀이공원", 30000,
            "사찰/문화재", 3000,
            "전망대", 15000,
            "기타 관광지", 8000
    );

    /** SYSTEM_PROMPT 값을 보관한다. */
    private static final String SYSTEM_PROMPT = """
        당신은 관광지 추천 전문 에이전트입니다.

        목표:
        사용자의 요청에 맞는 관광지 후보를 여러 개 추천합니다.

        사용 가능한 도구:
        1) searchAttractions: 관광지 검색(요약)
        2) fetchAttractionInfo: 후보 상세 정보 보완

        규칙:
        1) 관광지 후보는 최소 3개, 최대 6개를 제안하세요.
        2) entranceFee는 가능한 한 도구 결과에서 찾아 채우세요.
        3) 모르면 0으로 두세요.
        4) 반드시 JSON 배열만 출력하세요.
        """;

    /** USER_PROMPT_TEMPLATE 값을 보관한다. */
    private static final String USER_PROMPT_TEMPLATE = """
        사용자 요청: %s
        - 자연/문화/체험 등 다양한 유형을 섞어 관광지를 추천하세요.
        - 각 항목에는 name, address, description, entranceFee를 포함하세요.
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
    public TravelAttractionAgent(
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
    public List<Attraction> execute(String userQuery) {
        String userMessage = String.format(USER_PROMPT_TEMPLATE, userQuery);
        List<Attraction> result = callAsEntity(userMessage);
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
            query = String.format("%s 가성비 저렴한 관광지 추천", context.getDestination());
        } else {
            query = String.format("%s 관광지 추천", context.getDestination());
        }
        context.setAttractions(execute(query));
    }

    /**
     * callAsEntity 기능을 수행한다.
     *
     * @param userMessage userMessage 값
     * @return 조회 또는 생성된 목록
     */
    private List<Attraction> callAsEntity(String userMessage) {
        try {
            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage,
                    this,
                    new ParameterizedTypeReference<List<Attraction>>() {}
            );
        } catch (Exception first) {
            String repairMessage = """
                이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
                반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
                JSON 스키마: [{"name":"...","address":"...","description":"...","entranceFee":12345}]
                """;

            return runtimeModelChatService.callEntityWithTools(
                    RuntimeModelTarget.TRAVEL_SEARCH,
                    SYSTEM_PROMPT,
                    userMessage + "\n\n" + repairMessage,
                    this,
                    new ParameterizedTypeReference<List<Attraction>>() {}
            );
        }
    }

    /**
     * normalize 기능을 수행한다.
     *
     * @param items items 목록 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Attraction> normalize(List<Attraction> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Attraction> normalized = new ArrayList<>(items.size());
        for (Attraction a : items) {
            if (a == null || a.getName() == null || a.getName().isBlank()) {
                continue;
            }

            Integer fee = a.getEntranceFee();
            if (fee == null || fee <= 0) {
                int fallbackFee = inferDefaultFee(a);

                String desc = a.getDescription();
                String patchedDesc = (desc == null ? "" : desc);
                if (!patchedDesc.contains("기본요금") && !patchedDesc.contains("기본 입장료")) {
                    patchedDesc = patchedDesc.isBlank()
                            ? String.format(Locale.KOREAN, "기본 입장료 적용(%d원)", fallbackFee)
                            : String.format(Locale.KOREAN, "%s (기본 입장료 적용: %d원)", patchedDesc, fallbackFee);
                }

                a.setEntranceFee(fallbackFee);
                a.setDescription(patchedDesc);
            }
            normalized.add(a);
        }
        return normalized;
    }

    /**
     * inferDefaultFee 기능을 수행한다.
     *
     * @param a a 값
     * @return int 타입의 처리 결과
     */
    private int inferDefaultFee(Attraction a) {
        String name = a.getName() == null ? "" : a.getName();
        String desc = a.getDescription() == null ? "" : a.getDescription();
        String text = (name + " " + desc).trim();

        if (containsAny(text, "국립공원", "공원", "자연", "폭포", "해변", "산", "트레킹", "숲길", "호수")) {
            return DEFAULT_ENTRANCE_FEE.get("국립공원/자연명소");
        }
        if (containsAny(text, "박물관", "미술관", "전시", "갤러리")) {
            return DEFAULT_ENTRANCE_FEE.get("박물관/미술관");
        }
        if (containsAny(text, "테마파크", "놀이공원", "아쿠아리움", "워터파크")) {
            return DEFAULT_ENTRANCE_FEE.get("테마파크/놀이공원");
        }
        if (containsAny(text, "사찰", "절", "문화재", "유적", "궁", "성", "한옥")) {
            return DEFAULT_ENTRANCE_FEE.get("사찰/문화재");
        }
        if (containsAny(text, "전망대", "타워", "스카이", "뷰포인트")) {
            return DEFAULT_ENTRANCE_FEE.get("전망대");
        }
        return DEFAULT_ENTRANCE_FEE.get("기타 관광지");
    }

    /**
     * containsAny 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
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
    @Tool(description = "관광지 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
    public String searchAttractions(
            @ToolParam(description = "검색 쿼리 (예: '제주도 관광지', '서울 박물관')") String query
    ) {
        return webSearchPort.search(query);
    }

    /**
     * fetchAttractionInfo 기능을 수행한다.
     *
     * @param url 대상 URL
     * @return 처리 결과 문자열
     */
    @Tool(description = "웹 페이지의 본문 텍스트를 가져와 관광지 상세 정보를 제공합니다.")
    public String fetchAttractionInfo(@ToolParam(description = "웹 페이지 URL") String url) {
        return webSearchPort.fetch(url);
    }
}