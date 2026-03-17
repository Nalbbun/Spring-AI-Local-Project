package ai.local.nalbbun.internal.web;

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

import ai.local.nalbbun.internal.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.internal.model.llm.DebugOllamaModelActionRequest;
import ai.local.nalbbun.internal.model.llm.DebugOllamaModelActionResult;
import ai.local.nalbbun.internal.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.internal.model.llm.OllamaModelInfo;
import ai.local.nalbbun.internal.model.llm.OllamaModelSource;
import ai.local.nalbbun.internal.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.internal.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.internal.service.OllamaModelAdminService;
import ai.local.nalbbun.internal.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

/**
 * Debug Ollama Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/ollama")
public class DebugOllamaController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final OllamaModelAdminService ollamaModelAdminService;

    /**
     * get Connection 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/connection")
    public DebugOllamaConnectionInfo getConnection() {
        return ollamaModelDiscoveryService.getConnectionInfo();
    }

    /**
     * update Connection 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * reset Connection 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/connection/reset")
    public DebugOllamaConnectionInfo resetConnection() {
        ollamaConnectionService.reset();
        return ollamaModelDiscoveryService.getConnectionInfo();
    }

    /**
     * models 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/models")
    public List<OllamaModelInfo> models(
            @RequestParam(name = "source", defaultValue = "RUNNING") OllamaModelSource source
    ) {
        return ollamaModelDiscoveryService.getModels(source);
    }

    /**
     * model Action 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/models/action")
    public DebugOllamaModelActionResult modelAction(@RequestBody(required = false) DebugOllamaModelActionRequest request) {
        return ollamaModelAdminService.apply(request);
    }

    /**
     * get Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/config")
    public DebugOllamaModelConfig getConfig() {
        return debugRuntimeModelConfigService.getCurrentConfig();
    }

    /**
     * update Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/config")
    public DebugOllamaModelConfig updateConfig(@RequestBody DebugOllamaModelConfig request) {
        return debugRuntimeModelConfigService.update(request);
    }

    /**
     * reset Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/config/reset")
    public DebugOllamaModelConfig resetConfig() {
        return debugRuntimeModelConfigService.reset();
    }
}
