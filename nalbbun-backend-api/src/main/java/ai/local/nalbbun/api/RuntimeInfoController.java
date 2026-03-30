package ai.local.nalbbun.api;

import ai.local.nalbbun.admin.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.admin.service.OllamaModelDiscoveryService;
import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.runtime.OllamaConnectionInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runtime")
public class RuntimeInfoController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;

    @GetMapping("/ollama")
    public ApiResponse<OllamaConnectionInfoDto> ollamaConnectionInfo() {
        DebugOllamaConnectionInfo debugInfo = ollamaModelDiscoveryService.getConnectionInfo();
        return ApiResponse.ok(new OllamaConnectionInfoDto(
                debugInfo.getBaseUrl(),
                debugInfo.isReachable(),
                debugInfo.getStatus(),
                debugInfo.getMessage(),
                debugInfo.getRunningCount(),
                debugInfo.getInstalledCount()
        ));
    }
}
