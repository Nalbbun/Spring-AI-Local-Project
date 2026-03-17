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

/**
 * Ollama Model Admin Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class OllamaModelAdminService {

    private static final Logger log = LoggerFactory.getLogger(OllamaModelAdminService.class);

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * Ollama Model Admin Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public OllamaModelAdminService(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            OllamaModelDiscoveryService ollamaModelDiscoveryService
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.ollamaModelDiscoveryService = ollamaModelDiscoveryService;
    }

    /**
     * apply 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * pull Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
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

    /**
     * warmup Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
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

    /**
     * fill Counts 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void fillCounts(DebugOllamaModelActionResult result) {
        List<OllamaModelInfo> running = ollamaModelDiscoveryService.getRunningModels();
        List<OllamaModelInfo> installed = ollamaModelDiscoveryService.getInstalledModels();
        result.setRunningCount(running.size());
        result.setInstalledCount(installed.size());
        result.setRunningModels(running.stream().map(m -> m.getName() == null ? m.getModel() : m.getName()).toList());
    }

    /**
     * normalize Keep Alive 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String normalizeKeepAlive(String value) {
        if (value == null || value.isBlank()) {
            return "24h";
        }
        return value.trim();
    }

    /**
     * rest Client 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient restClient() {
        return RestClient.builder().baseUrl(ollamaConnectionService.getBaseUrl()).build();
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

    /**
     * Pull Request 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private record PullRequest(String model, boolean stream) {
        private PullRequest(String model) {
            this(model, false);
        }
    }

    /**
     * Generate Request 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private record GenerateRequest(String model, String prompt, boolean stream, String keep_alive) {
        private GenerateRequest(String model, String keepAlive) {
            this(model, "", false, keepAlive);
        }
    }

    /**
     * Embed Request 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private record EmbedRequest(String model, String input, String keep_alive) {
        private EmbedRequest(String model, String keepAlive) {
            this(model, "ping", keepAlive);
        }
    }
}
