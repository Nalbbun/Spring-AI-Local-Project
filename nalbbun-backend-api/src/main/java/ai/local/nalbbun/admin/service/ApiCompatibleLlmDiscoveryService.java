package ai.local.nalbbun.admin.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.admin.model.llm.DebugApiLlmConnectionInfo;

@Service
public class ApiCompatibleLlmDiscoveryService {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ApiCompatibleLlmDiscoveryService(
            @Value("${app.api-llm.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.api-llm.read-timeout-ms:15000}") int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public DebugApiLlmConnectionInfo inspect(
            String provider,
            String baseUrl,
            String apiKey,
            String keyProvider,
            String defaultModel,
            String healthCheckPath,
            String healthCheckMethod,
            String modelsPath,
            String modelsMethod) {

        DebugApiLlmConnectionInfo info = new DebugApiLlmConnectionInfo();
        info.setProvider(provider);
        info.setBaseUrl(baseUrl);
        info.setDefaultModel(defaultModel);
        info.setKeyProvider(keyProvider);
        info.setKeyResolved(apiKey != null && !apiKey.isBlank());
        info.setHealthCheckPath(normalizePath(healthCheckPath, "/models"));
        info.setHealthCheckMethod(normalizeMethod(healthCheckMethod, "GET"));
        info.setModelsPath(normalizePath(modelsPath, "/models"));
        info.setModelsMethod(normalizeMethod(modelsMethod, "GET"));
        info.setResolvedHealthUrl(joinUrl(baseUrl, info.getHealthCheckPath()));
        info.setResolvedModelsUrl(joinUrl(baseUrl, info.getModelsPath()));

        if (baseUrl == null || baseUrl.isBlank()) {
            info.setStatus("NOT_CONFIGURED");
            info.setMessage("Base URL이 비어 있습니다.");
            return info;
        }

        RestClient client = restClient(baseUrl, apiKey);
        List<String> messages = new ArrayList<>();

        try {
            exchange(client, info.getHealthCheckMethod(), info.getHealthCheckPath());
            info.setHealthCheckOk(true);
            messages.add("health ok");
        } catch (Exception e) {
            info.setHealthCheckOk(false);
            messages.add("health fail: " + compactMessage(e));
        }

        try {
            Map<String, Object> body = exchange(client, info.getModelsMethod(), info.getModelsPath());
            List<String> models = extractModels(body);
            info.setAvailableModels(models);
            info.setModelCount(models.size());
            info.setModelsCheckOk(true);
            messages.add(models.isEmpty() ? "models ok (empty)" : "models ok");
        } catch (Exception e) {
            info.setModelsCheckOk(false);
            messages.add("models fail: " + compactMessage(e));
        }

        info.setReachable(info.isHealthCheckOk() || info.isModelsCheckOk());

        if (info.isHealthCheckOk() && info.isModelsCheckOk()) {
            info.setStatus("CONNECTED");
        } else if (info.isReachable()) {
            info.setStatus("PARTIAL");
        } else {
            info.setStatus("ERROR");
        }

        info.setMessage(String.join(" | ", messages));
        return info;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(RestClient client, String method, String path) {
        String normalized = normalizeMethod(method, "GET");
        if ("POST".equals(normalized)) {
            return client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);
        }
        return client.get()
                .uri(path)
                .retrieve()
                .body(Map.class);
    }

    private RestClient restClient(String baseUrl, String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractModels(Map<String, Object> body) {
        List<String> result = new ArrayList<>();
        if (body == null) return result;
        Object data = body.get("data");
        if (data instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object id = map.get("id");
                    if (id != null) result.add(String.valueOf(id));
                }
            }
        }
        return result;
    }

    private String normalizePath(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) v = fallback;
        return v.startsWith("/") ? v : "/" + v;
    }

    private String normalizeMethod(String value, String fallback) {
        String v = value == null ? "" : value.trim().toUpperCase();
        if (!"GET".equals(v) && !"POST".equals(v)) return fallback;
        return v;
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String p = path == null ? "" : path.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!p.startsWith("/")) p = "/" + p;
        return base + p;
    }

    private String compactMessage(Exception e) {
        String message = e.getMessage();
        return (message == null || message.isBlank())
                ? e.getClass().getSimpleName()
                : message.replace("\n", " ");
    }
}
