package ai.local.nalbbun.admin.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.admin.model.llm.VllmChatTestRequest;
import ai.local.nalbbun.admin.model.llm.VllmEmbeddingTestRequest;
import ai.local.nalbbun.admin.model.llm.VllmRerankTestRequest;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VllmGatewayTestService {
    private final RuntimeVllmConnectionPort vllmConnectionPort;

    public Map<String, Object> syncFromInfo() {
        if (!(vllmConnectionPort instanceof DebugRuntimeVllmConnectionService service)) {
            throw new IllegalStateException("vLLM debug service를 찾을 수 없습니다.");
        }
        Map<String, Object> info = getInfo();
        Map<String, String> models = extractStringMap(info.get("models"));
        Map<String, String> endpoints = extractStringMap(info.get("endpoints"));
        service.update(
                stringValue(info.get("gateway_url"), service.getBaseUrl()),
                null,
                service.getKeyProvider(),
                "/api/info",
                "GET",
                "/v1/models",
                "GET",
                pathFromEndpoint(endpoints.get("preprocess (sLLM)"), "/sllm"),
                pathFromEndpoint(endpoints.get("chat_completion (LLM)"), "/llm"),
                pathFromEndpoint(endpoints.get("embedding"), "/embedding"),
                pathFromEndpoint(endpoints.get("rerank"), "/rerank"),
                mapAlias(models.get("sllm"), service.getSearchModel()),
                mapAlias(models.get("llm"), service.getAnswerModel()),
                mapAlias(models.get("embedding"), service.getEmbeddingModel()),
                mapAlias(models.get("rerank"), service.getRerankModel())
        );
        return info;
    }

    public Map<String, Object> testChat(VllmChatTestRequest request) {
        String mode = request.getMode() == null ? "LLM" : request.getMode().trim().toUpperCase();
        String baseUrl = "SLLM".equals(mode) ? vllmConnectionPort.getSllmBaseUrl() : vllmConnectionPort.getLlmBaseUrl();
        String model = request.getModel();
        if (model == null || model.isBlank()) {
            model = "SLLM".equals(mode) ? vllmConnectionPort.getSearchModel() : vllmConnectionPort.getAnswerModel();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", java.util.List.of(
                Map.of("role", "system", "content", stringValue(request.getSystemPrompt(), "당신은 유능한 AI 조수입니다.")),
                Map.of("role", "user", "content", stringValue(request.getUserPrompt(), "안녕하세요."))
        ));
        payload.put("temperature", 0.2);
        Map<String, Object> response = restClient(baseUrl).post().uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("request", payload);
        result.put("baseUrl", baseUrl);
        result.put("response", response);
        return result;
    }

    public Map<String, Object> testEmbedding(VllmEmbeddingTestRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("texts", request.getTexts());
        Map<String, Object> response = restClient(vllmConnectionPort.getEmbeddingBaseUrl()).post().uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        return Map.of("request", payload, "baseUrl", vllmConnectionPort.getEmbeddingBaseUrl(), "response", response);
    }

    public Map<String, Object> testRerank(VllmRerankTestRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", request.getQuery());
        payload.put("documents", request.getDocuments());
        payload.put("top_k", request.getTopK() == null ? 3 : request.getTopK());
        Map<String, Object> response = restClient(vllmConnectionPort.getRerankBaseUrl()).post().uri("/api/v1/rerank")
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        return Map.of("request", payload, "baseUrl", vllmConnectionPort.getRerankBaseUrl(), "response", response);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getInfo() {
        return restClient(vllmConnectionPort.getBaseUrl()).get().uri("/api/info").retrieve().body(Map.class);
    }

    private RestClient restClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(30000));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        String apiKey = vllmConnectionPort.getResolvedApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }
        return builder.build();
    }

    private Map<String, String> extractStringMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return result;
    }

    private String pathFromEndpoint(String endpoint, String fallback) {
        if (endpoint == null || endpoint.isBlank()) return fallback;
        String normalized = endpoint.trim();
        int slash = normalized.indexOf('/', normalized.indexOf("//") + 2);
        if (slash < 0) return fallback;
        String path = normalized.substring(slash);
        if (path.endsWith("/v1/chat/completions")) return path.substring(0, path.length() - "/v1/chat/completions".length());
        if (path.endsWith("/v1/embeddings")) return path.substring(0, path.length() - "/v1/embeddings".length());
        if (path.endsWith("/api/v1/rerank")) return path.substring(0, path.length() - "/api/v1/rerank".length());
        return path;
    }

    private String mapAlias(String source, String fallback) {
        String v = source == null ? "" : source.trim();
        if (v.isBlank()) return fallback;
        String n = v.toLowerCase();
        if (n.contains("exaone-3.5-2.4b")) return "exaone-3.5-2.4b-it";
        if (n.contains("exaone-3.5-32b")) return "exaone-3.5-32b-it";
        if (n.contains("bge-reranker-v2-m3")) return "bge-reranker-v2-m3";
        if (n.contains("bge-m3")) return "bge-m3";
        return v;
    }

    private String stringValue(Object value, String fallback) {
        String s = value == null ? "" : String.valueOf(value).trim();
        return s.isBlank() ? fallback : s;
    }
}
