package ai.local.nalbbun.debug.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/debug/api/ollama")
public class DebugOllamaController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;

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
}