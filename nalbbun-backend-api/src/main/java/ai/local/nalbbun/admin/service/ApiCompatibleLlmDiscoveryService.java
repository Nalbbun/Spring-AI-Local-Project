package ai.local.nalbbun.admin.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    public ApiCompatibleLlmDiscoveryService(@Value("${app.api-llm.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.api-llm.read-timeout-ms:15000}") int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public DebugApiLlmConnectionInfo inspect(String provider, String baseUrl, String apiKey, String keyProvider,
            String defaultModel, String healthCheckPath, String healthCheckMethod, String modelsPath, String modelsMethod) {
        DebugApiLlmConnectionInfo info = new DebugApiLlmConnectionInfo();
        info.setProvider(provider); info.setBaseUrl(baseUrl); info.setDefaultModel(defaultModel); info.setKeyProvider(keyProvider);
        info.setKeyResolved(apiKey != null && !apiKey.isBlank());
        info.setHealthCheckPath(normalizePath(healthCheckPath)); info.setHealthCheckMethod(normalizeMethod(healthCheckMethod, "GET"));
        info.setModelsPath(normalizePath(modelsPath)); info.setModelsMethod(normalizeMethod(modelsMethod, "GET"));
        info.setResolvedHealthUrl(joinUrl(baseUrl, info.getHealthCheckPath()));
        info.setResolvedModelsUrl(joinUrl(baseUrl, info.getModelsPath()));
        if (baseUrl == null || baseUrl.isBlank()) { info.setStatus("NOT_CONFIGURED"); info.setMessage("Base URL이 비어 있습니다."); return info; }
        RestClient client = restClient(baseUrl, apiKey);
        List<String> messages = new ArrayList<>();
        Map<String, Object> healthBody = null; Map<String, Object> modelsBody = null;
        try { healthBody = exchange(client, info.getHealthCheckMethod(), info.getHealthCheckPath()); info.setHealthCheckOk(true); messages.add("health ok"); }
        catch (Exception e) { info.setHealthCheckOk(false); messages.add("health fail: " + compactMessage(e)); }
        try { modelsBody = exchange(client, info.getModelsMethod(), info.getModelsPath()); info.setModelsCheckOk(true); messages.add("models ok"); }
        catch (Exception e) { info.setModelsCheckOk(false); messages.add("models fail: " + compactMessage(e)); }
        List<String> models = new ArrayList<>(); models.addAll(extractModels(modelsBody));
        Map<String,String> infoModels = extractStringMap(healthBody, "models");
        Map<String,String> infoEndpoints = extractStringMap(healthBody, "endpoints");
        info.setInfoModels(infoModels); info.setInfoEndpoints(infoEndpoints); models.addAll(infoModels.values());
        info.setAvailableModels(new ArrayList<>(new LinkedHashSet<>(models)));
        info.setModelCount(info.getAvailableModels().size());
        info.setReachable(info.isHealthCheckOk() || info.isModelsCheckOk());
        info.setStatus(info.isHealthCheckOk() && info.isModelsCheckOk() ? "CONNECTED" : (info.isReachable() ? "PARTIAL" : "ERROR"));
        info.setMessage(String.join(" | ", messages));
        return info;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> exchange(RestClient client, String method, String path) {
        String normalized = normalizeMethod(method, "GET");
        if ("POST".equals(normalized)) {
            return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(Map.of()).retrieve().body(Map.class);
        }
        return client.get().uri(path).retrieve().body(Map.class);
    }

    private RestClient restClient(String baseUrl, String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.isBlank()) builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractModels(Map<String,Object> body) {
        List<String> result = new ArrayList<>();
        if (body == null) return result;
        Object data = body.get("data");
        if (data instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?,?> map && map.get("id") != null) result.add(String.valueOf(map.get("id")));
            }
        }
        return result;
    }

    private Map<String,String> extractStringMap(Map<String,Object> body, String key) {
        Map<String,String> result = new LinkedHashMap<>();
        if (body == null) return result;
        Object value = body.get(key);
        if (value instanceof Map<?,?> map) {
            for (Map.Entry<?,?> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) result.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        return result;
    }

    private String normalizePath(String value) { String v = value == null ? "" : value.trim(); if (v.isBlank()) return "/models"; return v.startsWith("/") ? v : "/" + v; }
    private String normalizeMethod(String value, String fallback) { String v = value == null ? "" : value.trim().toUpperCase(); if (!"POST".equals(v) && !"GET".equals(v)) return fallback; return v; }
    private String joinUrl(String baseUrl, String path) { String base = baseUrl == null ? "" : baseUrl.trim(); String p = normalizePath(path); if (base.endsWith("/")) base = base.substring(0, base.length() - 1); return base + p; }
    private String compactMessage(Exception e) { String m = e.getMessage(); return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m.replace("\n", " "); }
}
