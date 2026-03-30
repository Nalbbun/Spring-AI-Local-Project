package ai.local.nalbbun.domain.category.travel.agent;

import ai.local.nalbbun.domain.category.travel.model.Accommodation;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.domain.search.port.WebSearchPort;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Travel Accommodation Agent 타입이다.
 *
 * <p>기능 설명: 특정 작업 목적에 맞는 세부 처리 단위를 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelAccommodationAgent {

    private static final Map<String, Integer> DEFAULT_PRICE_PER_NIGHT = Map.of(
            "호텔", 150000,
            "리조트", 180000,
            "펜션", 100000,
            "게스트하우스", 60000,
            "숙소", 120000
    );

    private static final String SYSTEM_PROMPT = """
        당신은 숙소 추천 전문 에이전트입니다.
        도구를 활용해 사용자의 요청에 맞는 숙소 후보를 추천하세요.
        반드시 JSON 배열만 출력하세요.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        사용자 요청: %s
        - 호텔/리조트/펜션/게스트하우스 등 다양한 유형을 섞어 추천하세요.
        - 각 항목에는 name, address, description, pricePerNight를 포함하세요.
        - 반드시 JSON 배열만 출력하세요.
        """;

    private final RuntimeModelChatService runtimeModelChatService;
    private final WebSearchPort webSearchPort;

    /**
     * Travel Accommodation Agent 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public TravelAccommodationAgent(
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
    public List<Accommodation> execute(String userQuery) {
        String userMessage = String.format(Locale.KOREAN, USER_PROMPT_TEMPLATE, userQuery);
        List<Accommodation> result = callAsEntity(userMessage);
        return normalize(result);
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
            query = String.format("%s 가성비 저렴한 숙소 추천", context.getDestination());
        } else {
            query = String.format("%s 숙소 추천", context.getDestination());
        }
        context.setAccommodations(execute(query));
    }

    /**
     * call As Entity 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * infer Default Price 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safe(String s) {
        return s == null ? "" : s;
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
     * search Accommodations 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Tool(description = "숙소 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
    public String searchAccommodations(@ToolParam(description = "검색 쿼리") String query) {
        return webSearchPort.search(query);
    }

    /**
     * fetch Accommodation Info 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Tool(description = "웹 페이지의 본문 텍스트를 가져와 숙소 상세 정보를 제공합니다.")
    public String fetchAccommodationInfo(@ToolParam(description = "웹 페이지 URL") String url) {
        return webSearchPort.fetch(url);
    }
}