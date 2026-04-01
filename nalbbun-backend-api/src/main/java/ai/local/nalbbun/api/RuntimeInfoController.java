package ai.local.nalbbun.api;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.admin.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.admin.service.OllamaModelDiscoveryService;
import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.runtime.OllamaConnectionInfoDto;
import ai.local.nalbbun.api.dto.runtime.RuntimeMetaDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runtime")
public class RuntimeInfoController {

    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;
    private final Environment environment;

    @Value("${app.debug.enabled:true}")
    private boolean debugEnabled;

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

    @GetMapping("/meta")
    public ApiResponse<RuntimeMetaDto> runtimeMeta() {
        boolean localProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equalsIgnoreCase);
        return ApiResponse.ok(new RuntimeMetaDto(
                debugEnabled,
                debugEnabled && localProfile,
                true,
                "HEADER_OR_QUERY_PARAM",
                Arrays.asList(environment.getActiveProfiles())
        ));
    }
}
