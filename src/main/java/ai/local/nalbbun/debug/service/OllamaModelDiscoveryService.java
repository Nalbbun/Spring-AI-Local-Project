package ai.local.nalbbun.debug.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OllamaModelDiscoveryService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: ollama model discovery service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class OllamaModelDiscoveryService {

    /** log 값을 보관한다. */
    private static final Logger log = LoggerFactory.getLogger(OllamaModelDiscoveryService.class);

    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    /** jsonMapper 값을 보관한다. */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ollamaConnectionService ollamaConnectionService 값
     */
    public OllamaModelDiscoveryService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 조회 또는 생성된 목록
     */
    public List<OllamaModelInfo> getRunningModels() {
        return fetchModelsSafely("/api/ps", "RUNNING");
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 조회 또는 생성된 목록
     */
    public List<OllamaModelInfo> getInstalledModels() {
        return fetchModelsSafely("/api/tags", "INSTALLED");
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param source source 값
     * @return 조회 또는 생성된 목록
     */
    public List<OllamaModelInfo> getModels(OllamaModelSource source) {
        if (source == OllamaModelSource.RUNNING) {
            return getRunningModels();
        }
        if (source == OllamaModelSource.INSTALLED) {
            return getInstalledModels();
        }

        Map<String, OllamaModelInfo> merged = new LinkedHashMap<>();
        for (OllamaModelInfo model : getInstalledModels()) {
            merged.put(keyOf(model), model);
        }
        for (OllamaModelInfo model : getRunningModels()) {
            String key = keyOf(model);
            OllamaModelInfo existing = merged.get(key);
            if (existing == null) {
                merged.put(key, model);
            } else {
                existing.setState("RUNNING+INSTALLED");
                existing.setDisplayName("[RUNNING+INSTALLED] " + nonBlank(existing.getName(), existing.getModel(), "unknown"));
                if (existing.getSize() == null && model.getSize() != null) {
                    existing.setSize(model.getSize());
                }
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(m -> nonBlank(m.getName(), m.getModel(), "zzzz")))
                .toList();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return DebugOllamaConnectionInfo 타입의 처리 결과
     */
    public DebugOllamaConnectionInfo getConnectionInfo() {
        DebugOllamaConnectionInfo info = new DebugOllamaConnectionInfo();
        info.setBaseUrl(ollamaConnectionService.getBaseUrl());
        info.setStatus("ERROR");
        info.setReachable(false);
        info.setMessage("not checked");

        try {
            restClient().get()
                    .uri("/api/version")
                    .retrieve()
                    .body(String.class);

            List<OllamaModelInfo> running = fetchModels("/api/ps", "RUNNING");
            List<OllamaModelInfo> installed = fetchModels("/api/tags", "INSTALLED");

            info.setReachable(true);
            info.setStatus("OK");
            info.setMessage("connected");
            info.setRunningCount(running.size());
            info.setInstalledCount(installed.size());
            info.setRunningModels(running.stream()
                    .map(model -> nonBlank(model.getName(), model.getModel(), "unknown"))
                    .toList());
        } catch (Exception e) {
            info.setReachable(false);
            info.setStatus("ERROR");
            info.setMessage(rootMessage(e));
            info.setRunningCount(0);
            info.setInstalledCount(0);
            info.setRunningModels(List.of());
            log.warn("Ollama debug connection failed. baseUrl={}, reason={}", info.getBaseUrl(), info.getMessage());
        }
        return info;
    }

    /**
     * fetchModelsSafely 기능을 수행한다.
     *
     * @param uri uri 값
     * @param state 현재 처리 상태 정보
     * @return 조회 또는 생성된 목록
     */
    private List<OllamaModelInfo> fetchModelsSafely(String uri, String state) {
        try {
            return fetchModels(uri, state);
        } catch (Exception e) {
            log.warn("Ollama model query failed. baseUrl={}, uri={}, reason={}", ollamaConnectionService.getBaseUrl(), uri, rootMessage(e));
            return List.of();
        }
    }

    /**
     * fetchModels 기능을 수행한다.
     *
     * @param uri uri 값
     * @param state 현재 처리 상태 정보
     * @return 조회 또는 생성된 목록
     */
    private List<OllamaModelInfo> fetchModels(String uri, String state) throws Exception {
        String body = restClient().get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return List.of();
        }

        JsonNode root = jsonMapper.readTree(body);
        JsonNode models = root.path("models");
        if (!models.isArray()) {
            return List.of();
        }

        List<OllamaModelInfo> result = new ArrayList<>();
        for (JsonNode node : models) {
            String name = nonBlank(node.path("name").asText(null), node.path("model").asText(null), "unknown");
            String model = nonBlank(node.path("model").asText(null), name, "unknown");
            Long size = node.hasNonNull("size") ? node.path("size").asLong() : null;
            OllamaModelInfo info = new OllamaModelInfo();
            info.setName(name);
            info.setModel(model);
            info.setSize(size);
            info.setState(state);
            info.setDisplayName("[" + state + "] " + name);
            result.add(info);
        }
        return result;
    }

    /**
     * restClient 기능을 수행한다.
     * @return RestClient 타입의 처리 결과
     */
    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .build();
    }

    /**
     * keyOf 기능을 수행한다.
     *
     * @param model 대상 모델 이름
     * @return 처리 결과 문자열
     */
    private String keyOf(OllamaModelInfo model) {
        return nonBlank(model.getName(), model.getModel(), "unknown");
    }

    /**
     * nonBlank 기능을 수행한다.
     *
     * @param values values 값
     * @return 처리 결과 문자열
     */
    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * rootMessage 기능을 수행한다.
     *
     * @param e e 값
     * @return 처리 결과 문자열
     */
    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }
}
