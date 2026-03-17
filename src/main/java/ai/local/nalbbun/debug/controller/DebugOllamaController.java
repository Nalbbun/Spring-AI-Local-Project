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
import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/ollama")
public class DebugOllamaController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    @GetMapping("/models")
    public List<OllamaModelInfo> models(
            @RequestParam(name = "source", defaultValue = "RUNNING") OllamaModelSource source
    ) {
        return ollamaModelDiscoveryService.getModels(source);
    }

    @GetMapping("/config")
    public DebugOllamaModelConfig getConfig() {
        return debugRuntimeModelConfigService.getCurrentConfig();
    }

    @PostMapping("/config")
    public DebugOllamaModelConfig updateConfig(@RequestBody DebugOllamaModelConfig request) {
        return debugRuntimeModelConfigService.update(request);
    }

    @PostMapping("/config/reset")
    public DebugOllamaModelConfig resetConfig() {
        return debugRuntimeModelConfigService.reset();
    }

    @GetMapping("/connection")
    public DebugOllamaConnectionInfo getConnection() {
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }

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
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }

    @PostMapping("/connection/reset")
    public DebugOllamaConnectionInfo resetConnection() {
        ollamaConnectionService.reset();
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }
}
