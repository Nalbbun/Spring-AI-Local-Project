package ai.local.nalbbun.service.search;

import ai.local.nalbbun.port.WebSearchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * DummyWebSearchService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: dummy web search service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.search", name = "provider", havingValue = "dummy", matchIfMissing = true)
public class DummyWebSearchService implements WebSearchPort {

    /**
     * providerName 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String providerName() {
        return "dummy";
    }


    /**
     * 대상 정보를 조회한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    @Override
    public String search(String query) {
        String q = normalize(query);

        if (containsAny(q, "관광", "명소", "가볼만한", "관광지", "여행지", "추천")) {
            return attractionResults(q);
        }

        if (containsAny(q, "맛집", "음식", "레스토랑", "식당", "먹을", "식사")) {
            return restaurantResults(q);
        }

        if (containsAny(q, "숙소", "호텔", "펜션", "리조트", "게스트하우스")) {
            return accommodationResults(q);
        }

        return """
            [안내] 현재 더미 검색은 아래 유형만 지원합니다.
            - 관광지
            - 맛집
            - 숙소
            예: 제주도 관광지 추천
            """;
    }

    /**
     * fetch 기능을 수행한다.
     *
     * @param url 대상 URL
     * @return 처리 결과 문자열
     */
    @Override
    public String fetch(String url) {
        String u = normalize(url);

        if (containsAny(u, "attraction", "명소", "관광")) {
            return "이 관광지는 매우 유명한 명소입니다. 입장료는 성인 기준 10,000원이며 운영 시간은 오전 9시부터 오후 6시까지입니다.";
        }

        if (containsAny(u, "restaurant", "맛집", "식당")) {
            return "이 식당은 현지 특색 요리로 유명합니다. 대표 메뉴는 1인 기준 15,000원이며 영업 시간은 오전 11시부터 오후 9시까지입니다.";
        }

        if (containsAny(u, "hotel", "숙소", "pension")) {
            return "이 숙소는 깨끗하고 편안한 시설을 갖추고 있으며 1박 요금은 80,000원부터 시작합니다. 체크인은 오후 3시입니다.";
        }

        return "페이지 내용을 가져왔습니다. 상세 설명이 포함되어 있습니다.";
    }

    /**
     * attractionResults 기능을 수행한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    private String attractionResults(String query) {
        return formatResults("관광지", List.of(
                "[1] 성산일출봉\n제주특별자치도 서귀포시 성산읍 일출로 284-12\n유네스코 세계자연유산. 입장료: 5,000원",
                "[2] 만장굴\n제주특별자치도 제주시 구좌읍 만장굴길 182\n세계적인 용암동굴. 입장료: 4,000원",
                "[3] 오설록 티뮤지엄\n제주특별자치도 서귀포시 안덕면 신화역사로 15\n녹차 문화 체험 공간. 입장료: 무료"
        ), query);
    }

    /**
     * restaurantResults 기능을 수행한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    private String restaurantResults(String query) {
        return formatResults("맛집", List.of(
                "[1] 올레국수\n제주특별자치도 제주시 귀아랑길 24\n고기국수 전문. 1인 평균 가격: 8,000원",
                "[2] 돈사돈\n제주특별자치도 제주시 노형로 256\n제주 흑돼지 전문점. 1인 평균 가격: 20,000원",
                "[3] 명진전복\n제주특별자치도 제주시 조천읍 신북로 532\n전복돌솥밥 전문. 1인 평균 가격: 17,000원"
        ), query);
    }

    /**
     * accommodationResults 기능을 수행한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    private String accommodationResults(String query) {
        return formatResults("숙소", List.of(
                "[1] 메종글래드제주\n제주특별자치도 제주시 노연로 80\n도심형 호텔. 1박 요금: 120,000원",
                "[2] 제주게스트하우스\n제주특별자치도 제주시 신대로 82\n가성비 숙소. 1박 요금: 35,000원",
                "[3] 협재비치호텔\n제주특별자치도 제주시 한림읍 한림로 329-10\n해변 인접 호텔. 1박 요금: 130,000원"
        ), query);
    }

    /**
     * formatResults 기능을 수행한다.
     *
     * @param type type 값
     * @param items items 목록 정보
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    private String formatResults(String type, List<String> items, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.KOREAN, "[더미 %s 검색 결과] query=%s%n%n", type, query));
        items.forEach(item -> sb.append(item).append("\n\n"));
        return sb.toString().trim();
    }

    /**
     * containsAny 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * normalize 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}