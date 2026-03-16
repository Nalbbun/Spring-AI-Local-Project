package ai.local.nalbbun.debug.controller;

import java.util.List;

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
import ai.local.nalbbun.debug.model.llm.DebugOllamaWarmupResult;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import ai.local.nalbbun.debug.service.OllamaRuntimeKeepAliveService;
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
    private final OllamaRuntimeKeepAliveService ollamaRuntimeKeepAliveService;

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
        DebugOllamaModelConfig updated = debugRuntimeModelConfigService.update(request);
        if (Boolean.TRUE.equals(updated.getAutoWarmupWhenNoRunningModels()) && updated.getResidentModels() != null && !updated.getResidentModels().isBlank()) {
            ollamaRuntimeKeepAliveService.warmupConfiguredResidentModels();
        }
        return updated;
    }

    @PostMapping("/config/reset")
    public DebugOllamaModelConfig resetConfig() {
        return debugRuntimeModelConfigService.reset();
    }

    @PostMapping("/resident-models/apply")
    public DebugOllamaWarmupResult applyResidentModels() {
        return ollamaRuntimeKeepAliveService.warmupConfiguredResidentModels();
    }

    @GetMapping("/connection")
    public DebugOllamaConnectionInfo getConnection() {
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }

    @PostMapping("/connection")
    public DebugOllamaConnectionInfo updateConnection(@RequestBody DebugOllamaConnectionInfo request) {
        String baseUrl = null;
        if (request != null) {
            baseUrl = request.getBaseUrl();
            if ((baseUrl == null || baseUrl.isBlank()) && request.getDefaultBaseUrl() != null && !request.getDefaultBaseUrl().isBlank()) {
                baseUrl = request.getDefaultBaseUrl();
            }
        }
        ollamaConnectionService.update(baseUrl);
        if (debugRuntimeModelConfigService.isAutoWarmupWhenNoRunningModels() && !debugRuntimeModelConfigService.getResidentModelList().isEmpty()) {
            try {
                ollamaRuntimeKeepAliveService.warmupConfiguredResidentModels();
            } catch (Exception ignored) {
            }
        }
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }

    @PostMapping("/connection/reset")
    public DebugOllamaConnectionInfo resetConnection() {
        ollamaConnectionService.reset();
        return ollamaModelDiscoveryService.getDebugConnectionInfo();
    }
}
