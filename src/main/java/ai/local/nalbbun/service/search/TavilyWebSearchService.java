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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import ai.local.nalbbun.port.WebSearchPort;
import lombok.extern.slf4j.Slf4j;

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

    public TavilyWebSearchService(
            @Value("${app.search.tavily.api-key:}") String apiKey,
            @Value("${app.search.tavily.max-results:5}") int maxResults
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.maxResults = Math.max(1, maxResults);
    }

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

    @Override
    public String providerName() {
        return "tavily";
    }

    private void ensureApiKey() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("app.search.provider=tavily 인 경우 app.search.tavily.api-key 설정이 필요합니다.");
        }
    }

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
