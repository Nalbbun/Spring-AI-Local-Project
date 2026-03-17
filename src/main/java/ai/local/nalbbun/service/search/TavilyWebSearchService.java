package ai.local.nalbbun.service.search;

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

import ai.local.nalbbun.port.WebSearchPort;
import lombok.extern.slf4j.Slf4j;

/**
 * TavilyWebSearchService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: tavily web search service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.search", name = "provider", havingValue = "tavily")
public class TavilyWebSearchService implements WebSearchPort {

    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** httpClient 값을 보관한다. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** apiKey 값을 보관한다. */
    private final String apiKey;
    /** maxResults 값을 보관한다. */
    private final int maxResults;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param apiKey apiKey 값
     * @param maxResults maxResults 값
     */
    public TavilyWebSearchService(
            @Value("${app.search.tavily.api-key:}") String apiKey,
            @Value("${app.search.tavily.max-results:5}") int maxResults
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.maxResults = Math.max(1, maxResults);
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
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
     * @param url 대상 URL
     * @return 처리 결과 문자열
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
     * providerName 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String providerName() {
        return "tavily";
    }

    /**
     * ensureApiKey 기능을 수행한다.
     */
    private void ensureApiKey() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("app.search.provider=tavily 인 경우 app.search.tavily.api-key 설정이 필요합니다.");
        }
    }

    /**
     * 현재 상태를 다른 표현 형태로 변환한다.
     *
     * @param html html 값
     * @return 처리 결과 문자열
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
