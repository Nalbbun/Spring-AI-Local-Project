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
        String syncBaseUrl = stringValue(info.get("gateway_url"), service.getBaseUrl());
        String sllmServedName = firstNonBlank(models.get("sllm_served_name"), models.get("sllm"), service.getSearchModel());
        String llmServedName = firstNonBlank(models.get("llm_served_name"), models.get("llm"), service.getAnswerModel());
        String embeddingName = firstNonBlank(models.get("embedding"), service.getEmbeddingModel());
        String rerankName = firstNonBlank(models.get("rerank"), service.getRerankModel());

        service.update(
                syncBaseUrl,
                llmServedName,
                service.getKeyProvider(),
                "/api/info",
                "GET",
                "/api/info",
                "GET",
                pathFromEndpoint(endpoints.get("preprocess (sLLM)"), "/sllm"),
                pathFromEndpoint(endpoints.get("chat_completion (LLM)"), "/llm"),
                pathFromEndpoint(endpoints.get("embedding"), "/embedding/api"),
                pathFromEndpoint(endpoints.get("rerank"), "/rerank"),
                sllmServedName,
                llmServedName,
                embeddingName,
                rerankName
        );
        return info;
    }

    public Map<String, Object> testChat(VllmChatTestRequest request) {
        String mode = request.getMode() == null ? "LLM" : request.getMode().trim().toUpperCase();
        String requestUrl = "SLLM".equals(mode) ? vllmConnectionPort.getResolvedSllmRequestUrl() : vllmConnectionPort.getResolvedLlmRequestUrl();
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
        Map<String, Object> response = restClientForAbsoluteUrl().post().uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("request", payload);
        result.put("requestUrl", requestUrl);
        result.put("response", response);
        return result;
    }

    public Map<String, Object> testEmbedding(VllmEmbeddingTestRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("texts", request.getTexts());
        String requestUrl = vllmConnectionPort.getResolvedEmbeddingRequestUrl();
        Map<String, Object> response = restClientForAbsoluteUrl().post().uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        return Map.of("request", payload, "requestUrl", requestUrl, "response", response);
    }

    public Map<String, Object> testRerank(VllmRerankTestRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", request.getQuery());
        payload.put("documents", request.getDocuments());
        payload.put("top_k", request.getTopK() == null ? 3 : request.getTopK());
        String requestUrl = vllmConnectionPort.getResolvedRerankRequestUrl();
        Map<String, Object> response = restClientForAbsoluteUrl().post().uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        return Map.of("request", payload, "requestUrl", requestUrl, "response", response);
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


    private RestClient restClientForAbsoluteUrl() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(30000));
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory)
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
        if (path.endsWith("/chat/completions")) return path.substring(0, path.length() - "/chat/completions".length());
        if (path.endsWith("/api/v1/embeddings")) return path.substring(0, path.length() - "/v1/embeddings".length());
        if (path.endsWith("/v1/embeddings")) return path.substring(0, path.length() - "/v1/embeddings".length());
        if (path.endsWith("/rerank/rerank")) return path.substring(0, path.length() - "/rerank".length());
        if (path.endsWith("/api/v1/rerank")) return path.substring(0, path.length() - "/api/v1/rerank".length());
        return path;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String v = value == null ? "" : value.trim();
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private String stringValue(Object value, String fallback) {
        String s = value == null ? "" : String.valueOf(value).trim();
        return s.isBlank() ? fallback : s;
    }
}
