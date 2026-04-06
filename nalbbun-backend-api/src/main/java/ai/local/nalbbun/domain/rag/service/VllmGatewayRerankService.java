package ai.local.nalbbun.domain.rag.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VllmGatewayRerankService {

    private final RuntimeVllmConnectionPort connectionPort;

    public boolean isAvailable() {
        return connectionPort.getBaseUrl() != null && !connectionPort.getBaseUrl().isBlank()
                && connectionPort.getRerankPath() != null && !connectionPort.getRerankPath().isBlank();
    }

    public List<RagRetrievedDocument> rerank(String query, List<RagRetrievedDocument> candidates) {
        if (!isAvailable() || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("query", query == null ? "" : query);
            payload.put("documents", candidates.stream().map(doc -> ((doc.title() == null ? "" : doc.title()) + "\n" + (doc.text() == null ? "" : doc.text())).trim()).toList());
            payload.put("top_k", candidates.size());
            String requestUrl = connectionPort.getResolvedRerankRequestUrl();
            Map<String, Object> response = restClient().post().uri(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            List<ScoredIndex> scored = parseScored(response);
            if (scored.isEmpty()) {
                return candidates;
            }
            List<RagRetrievedDocument> ranked = new ArrayList<>();
            for (ScoredIndex item : scored) {
                if (item.index() >= 0 && item.index() < candidates.size()) {
                    ranked.add(candidates.get(item.index()));
                }
            }
            return ranked.isEmpty() ? candidates : ranked;
        } catch (Exception e) {
            log.warn("vLLM rerank gateway call failed. reason={}", e.getMessage());
            return candidates;
        }
    }

    private RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        String apiKey = connectionPort.getResolvedApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<ScoredIndex> parseScored(Map<String, Object> response) {
        Object raw = firstNonNull(response.get("results"), response.get("data"), response.get("result"), response.get("rerank"));
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<ScoredIndex> scored = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Integer index = asInt(firstNonNull(map.get("index"), map.get("document_index"), map.get("idx")));
                Double score = asDouble(firstNonNull(map.get("score"), map.get("relevance_score"), map.get("similarity")));
                if (index != null) {
                    scored.add(new ScoredIndex(index, score == null ? 0d : score));
                }
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredIndex::score).reversed());
        return scored;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object value : values) if (value != null) return value;
        return null;
    }

    private Integer asInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return value == null ? null : Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private Double asDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try { return value == null ? null : Double.parseDouble(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private record ScoredIndex(int index, double score) {}
}
