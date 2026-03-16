package ai.local.nalbbun.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.OllamaConnectionInfo;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runtime")
public class RuntimeInfoController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;

    @GetMapping("/ollama")
    public OllamaConnectionInfo ollamaConnectionInfo() {
        DebugOllamaConnectionInfo debugInfo = ollamaModelDiscoveryService.getDebugConnectionInfo();
        OllamaConnectionInfo info = new OllamaConnectionInfo();
        info.setBaseUrl(debugInfo.getBaseUrl());
        info.setReachable(debugInfo.isReachable());
        info.setStatus(debugInfo.getStatus());
        info.setMessage(debugInfo.getMessage());
        info.setRunningCount(debugInfo.getRunningCount());
        info.setInstalledCount(debugInfo.getInstalledCount());
        return info;
    }
}
