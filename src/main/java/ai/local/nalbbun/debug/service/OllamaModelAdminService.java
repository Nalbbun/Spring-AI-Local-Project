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
 * OllamaModelAdminService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: ollama model admin service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class OllamaModelAdminService {

    /** log 값을 보관한다. */
    private static final Logger log = LoggerFactory.getLogger(OllamaModelAdminService.class);

    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    /** ollamaModelDiscoveryService 값을 보관한다. */
    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    /** jsonMapper 값을 보관한다. */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ollamaConnectionService ollamaConnectionService 값
     * @param ollamaModelDiscoveryService ollamaModelDiscoveryService 값
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
     * @param request HTTP 요청 객체
     * @return DebugOllamaModelActionResult 타입의 처리 결과
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
     * pullModel 기능을 수행한다.
     *
     * @param model 대상 모델 이름
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
     * warmupModel 기능을 수행한다.
     *
     * @param model 대상 모델 이름
     * @param keepAlive keepAlive 값
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
     * fillCounts 기능을 수행한다.
     *
     * @param result 처리 결과 객체
     */
    private void fillCounts(DebugOllamaModelActionResult result) {
        List<OllamaModelInfo> running = ollamaModelDiscoveryService.getRunningModels();
        List<OllamaModelInfo> installed = ollamaModelDiscoveryService.getInstalledModels();
        result.setRunningCount(running.size());
        result.setInstalledCount(installed.size());
        result.setRunningModels(running.stream().map(m -> m.getName() == null ? m.getModel() : m.getName()).toList());
    }

    /**
     * normalizeKeepAlive 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String normalizeKeepAlive(String value) {
        if (value == null || value.isBlank()) {
            return "24h";
        }
        return value.trim();
    }

    /**
     * restClient 기능을 수행한다.
     * @return RestClient 타입의 처리 결과
     */
    private RestClient restClient() {
        return RestClient.builder().baseUrl(ollamaConnectionService.getBaseUrl()).build();
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

    /**
     * PullRequest는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
     * <p>주요 기능: pull request 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     * @param model 대상 모델 이름
     * @param stream stream 값
     */
    private record PullRequest(String model, boolean stream) {
        /**
         * 필수 의존성을 주입하여 객체를 생성한다.
         *
         * @param model 대상 모델 이름
         */
        private PullRequest(String model) {
            this(model, false);
        }
    }

    /**
     * GenerateRequest는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
     * <p>주요 기능: generate request 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     * @param model 대상 모델 이름
     * @param prompt 사용자 입력 또는 질의 내용
     * @param stream stream 값
     * @param keep_alive keep_alive 값
     */
    private record GenerateRequest(String model, String prompt, boolean stream, String keep_alive) {
        /**
         * 필수 의존성을 주입하여 객체를 생성한다.
         *
         * @param model 대상 모델 이름
         * @param keepAlive keepAlive 값
         */
        private GenerateRequest(String model, String keepAlive) {
            this(model, "", false, keepAlive);
        }
    }

    /**
     * EmbedRequest는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
     * <p>주요 기능: embed request 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     * @param model 대상 모델 이름
     * @param input 입력 데이터
     * @param keep_alive keep_alive 값
     */
    private record EmbedRequest(String model, String input, String keep_alive) {
        /**
         * 필수 의존성을 주입하여 객체를 생성한다.
         *
         * @param model 대상 모델 이름
         * @param keepAlive keepAlive 값
         */
        private EmbedRequest(String model, String keepAlive) {
            this(model, "ping", keepAlive);
        }
    }
}
