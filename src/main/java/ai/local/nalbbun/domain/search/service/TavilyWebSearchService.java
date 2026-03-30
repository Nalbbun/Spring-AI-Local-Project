package ai.local.nalbbun.domain.search.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.local.nalbbun.domain.search.port.WebSearchPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Tavily Web Search Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.search", name = "provider", havingValue = "tavily")
public class TavilyWebSearchService implements WebSearchPort {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey;
    private final int maxResults;

    /**
     * Tavily Web Search Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public TavilyWebSearchService(
            @Value("${app.search.tavily.api-key:}") String apiKey,
            @Value("${app.search.tavily.max-results:5}") int maxResults
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.maxResults = Math.max(1, maxResults);
    }

    /**
     * search 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String search(String query) {
        ensureApiKey();
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("search_depth", "basic");
            requestBody.put("max_results", maxResults);
            requestBody.put("include_answer", true);
            requestBody.put("include_raw_content", false);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.tavily.com/search"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            StringBuilder sb = new StringBuilder();
            if (root.hasNonNull("answer")) {
                sb.append("[요약]\n").append(root.get("answer").asText()).append("\n\n");
            }

            JsonNode results = root.path("results");
            for (int i = 0; i < results.size(); i++) {
                JsonNode item = results.get(i);
                sb.append("[").append(i + 1).append("] ")
                        .append(item.path("title").asText("제목 없음")).append("\n")
                        .append(item.path("url").asText("URL 없음")).append("\n")
                        .append(item.path("content").asText("요약 없음")).append("\n\n");
            }

            return sb.toString().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tavily 검색 호출이 인터럽트되었습니다: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Tavily 검색 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * fetch 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return toPlainText(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("웹 본문 fetch가 인터럽트되었습니다: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("웹 본문 fetch에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * provider Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String providerName() {
        return "tavily";
    }

    /**
     * ensure Api Key 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void ensureApiKey() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("app.search.provider=tavily 인 경우 app.search.tavily.api-key 설정이 필요합니다.");
        }
    }

    /**
     * to Plain Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String text = html
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();

        if (text.length() > 3000) {
            return text.substring(0, 3000) + "...";
        }
        return text;
    }
}
