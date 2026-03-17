package ai.local.nalbbun.debug.controller;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.DebugOllamaModelActionRequest;
import ai.local.nalbbun.debug.model.llm.DebugOllamaModelActionResult;
import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.debug.service.OllamaModelAdminService;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

/**
 * DebugOllamaController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug ollama controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/ollama")
public class DebugOllamaController {

    /** ollamaModelDiscoveryService 값을 보관한다. */
    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    /** debugRuntimeModelConfigService 값을 보관한다. */
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    /** ollamaModelAdminService 값을 보관한다. */
    private final OllamaModelAdminService ollamaModelAdminService;

    /**
     * 지정된 정보를 조회한다.
     * @return DebugOllamaConnectionInfo 타입의 처리 결과
     */
    @GetMapping("/connection")
    public DebugOllamaConnectionInfo getConnection() {
        return ollamaModelDiscoveryService.getConnectionInfo();
    }

    /**
     * 대상 값을 갱신한다.
     *
     * @param request HTTP 요청 객체
     * @return DebugOllamaConnectionInfo 타입의 처리 결과
     */
    @PostMapping("/connection")
    public DebugOllamaConnectionInfo updateConnection(@RequestBody(required = false) Map<String, Object> request) {
        String baseUrl = null;
        if (request != null) {
            Object direct = request.get("baseUrl");
            Object legacy = request.get("ollamaBaseUrl");
            Object chosen = direct != null ? direct : legacy;
            baseUrl = chosen == null ? null : String.valueOf(chosen);
        }
        ollamaConnectionService.update(baseUrl);
        return ollamaModelDiscoveryService.getConnectionInfo();
    }

    /**
     * resetConnection 기능을 수행한다.
     * @return DebugOllamaConnectionInfo 타입의 처리 결과
     */
    @PostMapping("/connection/reset")
    public DebugOllamaConnectionInfo resetConnection() {
        ollamaConnectionService.reset();
        return ollamaModelDiscoveryService.getConnectionInfo();
    }

    /**
     * models 기능을 수행한다.
     *
     * @param source source 값
     * @return 조회 또는 생성된 목록
     */
    @GetMapping("/models")
    public List<OllamaModelInfo> models(
            @RequestParam(name = "source", defaultValue = "RUNNING") OllamaModelSource source
    ) {
        return ollamaModelDiscoveryService.getModels(source);
    }

    /**
     * modelAction 기능을 수행한다.
     *
     * @param request HTTP 요청 객체
     * @return DebugOllamaModelActionResult 타입의 처리 결과
     */
    @PostMapping("/models/action")
    public DebugOllamaModelActionResult modelAction(@RequestBody(required = false) DebugOllamaModelActionRequest request) {
        return ollamaModelAdminService.apply(request);
    }

    /**
     * 지정된 정보를 조회한다.
     * @return DebugOllamaModelConfig 타입의 처리 결과
     */
    @GetMapping("/config")
    public DebugOllamaModelConfig getConfig() {
        return debugRuntimeModelConfigService.getCurrentConfig();
    }

    /**
     * 대상 값을 갱신한다.
     *
     * @param request HTTP 요청 객체
     * @return DebugOllamaModelConfig 타입의 처리 결과
     */
    @PostMapping("/config")
    public DebugOllamaModelConfig updateConfig(@RequestBody DebugOllamaModelConfig request) {
        return debugRuntimeModelConfigService.update(request);
    }

    /**
     * resetConfig 기능을 수행한다.
     * @return DebugOllamaModelConfig 타입의 처리 결과
     */
    @PostMapping("/config/reset")
    public DebugOllamaModelConfig resetConfig() {
        return debugRuntimeModelConfigService.reset();
    }
}
