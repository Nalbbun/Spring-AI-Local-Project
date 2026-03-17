package ai.local.nalbbun.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.OllamaConnectionInfo;
import ai.local.nalbbun.debug.service.OllamaModelDiscoveryService;
import lombok.RequiredArgsConstructor;

/**
 * RuntimeInfoController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: runtime info controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runtime")
public class RuntimeInfoController {

    /** ollamaModelDiscoveryService 값을 보관한다. */
    private final OllamaModelDiscoveryService ollamaModelDiscoveryService;

    /**
     * ollamaConnectionInfo 기능을 수행한다.
     * @return OllamaConnectionInfo 타입의 처리 결과
     */
    @GetMapping("/ollama")
    public OllamaConnectionInfo ollamaConnectionInfo() {
        DebugOllamaConnectionInfo debugInfo = ollamaModelDiscoveryService.getConnectionInfo();
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
