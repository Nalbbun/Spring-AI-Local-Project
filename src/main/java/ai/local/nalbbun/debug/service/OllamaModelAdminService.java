package ai.local.nalbbun.debug.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelActionRequest;
import ai.local.nalbbun.debug.model.llm.DebugOllamaModelActionResult;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OllamaModelAdminService {

    private static final Logger log = LoggerFactory.getLogger(OllamaModelAdminService.class);

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OllamaModelAdminService(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            OllamaModelDiscoveryService ollamaModelDiscoveryService
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.ollamaModelDiscoveryService = ollamaModelDiscoveryService;
    }

    public DebugOllamaModelActionResult apply(DebugOllamaModelActionRequest request) {
        DebugOllamaModelActionResult result = new DebugOllamaModelActionResult();
        result.setBaseUrl(ollamaConnectionService.getBaseUrl());
        String model = request == null || request.getModel() == null ? "" : request.getModel().trim();
        boolean pull = request != null && Boolean.TRUE.equals(request.getPull());
        String keepAlive = normalizeKeepAlive(request == null ? null : request.getKeepAlive());

        result.setModel(model);
        result.setKeepAlive(keepAlive);
        result.setAction(pull ? "PULL" : "LOAD_TO_PS");

        if (model.isBlank()) {
            result.setSuccess(false);
            result.setMessage("모델명을 입력하세요.");
            fillCounts(result);
            return result;
        }

        try {
            if (pull) {
                pullModel(model);
                result.setSuccess(true);
                result.setMessage("모델 pull 완료");
            } else {
                warmupModel(model, keepAlive);
                result.setSuccess(true);
                result.setMessage("PS 로드 완료");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(rootMessage(e));
            log.warn("Ollama model admin failed. baseUrl={}, model={}, action={}, reason={}",
                    result.getBaseUrl(), model, result.getAction(), result.getMessage());
        }

        fillCounts(result);
        return result;
    }

    private void pullModel(String model) throws Exception {
        RestClient client = restClient();
        String body = jsonMapper.writeValueAsString(new PullRequest(model));
        client.post()
                .uri("/api/pull")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private void warmupModel(String model, String keepAlive) throws Exception {
        RestClient client = restClient();
        String generateBody = jsonMapper.writeValueAsString(new GenerateRequest(model, keepAlive));
        try {
            client.post()
                    .uri("/api/generate")
                    .header("Content-Type", "application/json")
                    .body(generateBody)
                    .retrieve()
                    .body(String.class);
            return;
        } catch (Exception ignored) {
            // embedding 모델일 수 있으므로 embed 로 재시도
        }

        String embedBody = jsonMapper.writeValueAsString(new EmbedRequest(model, keepAlive));
        client.post()
                .uri("/api/embed")
                .header("Content-Type", "application/json")
                .body(embedBody)
                .retrieve()
                .body(String.class);
    }

    private void fillCounts(DebugOllamaModelActionResult result) {
        List<OllamaModelInfo> running = ollamaModelDiscoveryService.getRunningModels();
        List<OllamaModelInfo> installed = ollamaModelDiscoveryService.getInstalledModels();
        result.setRunningCount(running.size());
        result.setInstalledCount(installed.size());
        result.setRunningModels(running.stream().map(m -> m.getName() == null ? m.getModel() : m.getName()).toList());
    }

    private String normalizeKeepAlive(String value) {
        if (value == null || value.isBlank()) {
            return "24h";
        }
        return value.trim();
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl(ollamaConnectionService.getBaseUrl()).build();
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private record PullRequest(String model, boolean stream) {
        private PullRequest(String model) {
            this(model, false);
        }
    }

    private record GenerateRequest(String model, String prompt, boolean stream, String keep_alive) {
        private GenerateRequest(String model, String keepAlive) {
            this(model, "", false, keepAlive);
        }
    }

    private record EmbedRequest(String model, String input, String keep_alive) {
        private EmbedRequest(String model, String keepAlive) {
            this(model, "ping", keepAlive);
        }
    }
}
