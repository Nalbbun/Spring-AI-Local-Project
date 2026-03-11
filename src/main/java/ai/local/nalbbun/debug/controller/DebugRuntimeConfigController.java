package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/debug/api/config")
public class DebugRuntimeConfigController {

    private final DebugRuntimeConfigService debugRuntimeConfigService;

    @GetMapping
    public DebugRuntimeConfig getConfig() {
        return debugRuntimeConfigService.getCurrentConfig();
    }

    @PostMapping
    public DebugRuntimeConfig updateConfig(@RequestBody DebugRuntimeConfig request) {
        return debugRuntimeConfigService.update(request);
    }

    @PostMapping("/reset")
    public DebugRuntimeConfig resetConfig() {
        return debugRuntimeConfigService.reset();
    }
}