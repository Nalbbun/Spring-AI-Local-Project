package ai.local.nalbbun.admin.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.admin.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.admin.model.llm.OllamaModelInfo;
import ai.local.nalbbun.admin.model.llm.OllamaModelSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ollama Model Discovery Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class OllamaModelDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(OllamaModelDiscoveryService.class);

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * Ollama Model Discovery Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public OllamaModelDiscoveryService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
    }

    /**
     * Running Models 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<OllamaModelInfo> getRunningModels() {
        return fetchModelsSafely("/api/ps", "RUNNING");
    }

    /**
     * Installed Models 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<OllamaModelInfo> getInstalledModels() {
        return fetchModelsSafely("/api/tags", "INSTALLED");
    }

    /**
     * Models 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * Connection Info 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * fetch Models Safely 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * fetch Models 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * rest Client 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .build();
    }

    /**
     * key Of 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String keyOf(OllamaModelInfo model) {
        return nonBlank(model.getName(), model.getModel(), "unknown");
    }

    /**
     * non Blank 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * root Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
