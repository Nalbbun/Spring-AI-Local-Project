package ai.local.nalbbun.admin.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.admin.model.llm.DebugApiLlmConnectionInfo;
import ai.local.nalbbun.admin.model.llm.DebugApiLlmProviderConfig;
import ai.local.nalbbun.admin.service.ApiCompatibleLlmDiscoveryService;
import ai.local.nalbbun.admin.service.DebugRuntimeOpenAiConnectionService;
import ai.local.nalbbun.admin.service.DebugRuntimeVllmConnectionService;
import ai.local.nalbbun.admin.service.VllmGatewayTestService;
import ai.local.nalbbun.admin.model.llm.VllmChatTestRequest;
import ai.local.nalbbun.admin.model.llm.VllmEmbeddingTestRequest;
import ai.local.nalbbun.admin.model.llm.VllmRerankTestRequest;
import lombok.RequiredArgsConstructor;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/llm")
public class DebugLlmProviderController {
    private final ApiCompatibleLlmDiscoveryService discoveryService;
    private final DebugRuntimeVllmConnectionService vllmConnectionService;
    private final DebugRuntimeOpenAiConnectionService openAiConnectionService;
    private final VllmGatewayTestService vllmGatewayTestService;

    @GetMapping("/providers/status")
    public Map<String, DebugApiLlmConnectionInfo> status() { Map<String, DebugApiLlmConnectionInfo> r = new LinkedHashMap<>(); r.put("vllm", vllmStatus()); r.put("openai", openAiStatus()); return r; }

    @GetMapping("/providers/vllm")
    public DebugApiLlmConnectionInfo vllmStatus() {
        DebugApiLlmConnectionInfo info = discoveryService.inspect("VLLM", vllmConnectionService.getBaseUrl(), vllmConnectionService.getResolvedApiKey(), vllmConnectionService.getKeyProvider(), vllmConnectionService.getConfiguredOrDefaultModel(), vllmConnectionService.getHealthCheckPath(), vllmConnectionService.getHealthCheckMethod(), vllmConnectionService.getModelsPath(), vllmConnectionService.getModelsMethod(), true);
        info.setSllmPath(vllmConnectionService.getSllmPath()); info.setLlmPath(vllmConnectionService.getLlmPath()); info.setEmbeddingPath(vllmConnectionService.getEmbeddingPath()); info.setRerankPath(vllmConnectionService.getRerankPath());
        info.setSearchModel(vllmConnectionService.getSearchModel()); info.setAnswerModel(vllmConnectionService.getAnswerModel()); info.setEmbeddingModel(vllmConnectionService.getEmbeddingModel()); info.setRerankModel(vllmConnectionService.getRerankModel());
        info.setResolvedSllmUrl(vllmConnectionService.getResolvedSllmRequestUrl());
        info.setResolvedLlmUrl(vllmConnectionService.getResolvedLlmRequestUrl());
        info.setResolvedEmbeddingUrl(vllmConnectionService.getResolvedEmbeddingRequestUrl());
        info.setResolvedRerankUrl(vllmConnectionService.getResolvedRerankRequestUrl());
        return info;
    }

    @PostMapping("/providers/vllm")
    public DebugApiLlmConnectionInfo updateVllm(@RequestBody(required = false) DebugApiLlmProviderConfig request) {
        vllmConnectionService.update(request == null ? null : request.getBaseUrl(), request == null ? null : request.getDefaultModel(), request == null ? null : request.getKeyProvider(), request == null ? null : request.getHealthCheckPath(), request == null ? null : request.getHealthCheckMethod(), request == null ? null : request.getModelsPath(), request == null ? null : request.getModelsMethod(), request == null ? null : request.getSllmPath(), request == null ? null : request.getLlmPath(), request == null ? null : request.getEmbeddingPath(), request == null ? null : request.getRerankPath(), request == null ? null : request.getSearchModel(), request == null ? null : request.getAnswerModel(), request == null ? null : request.getEmbeddingModel(), request == null ? null : request.getRerankModel());
        return vllmStatus();
    }

    @PostMapping("/providers/vllm/reset") public DebugApiLlmConnectionInfo resetVllm() { vllmConnectionService.reset(); return vllmStatus(); }

    @PostMapping("/providers/vllm/sync-info")
    public DebugApiLlmConnectionInfo syncVllmFromInfo() {
        vllmGatewayTestService.syncFromInfo();
        return vllmStatus();
    }

    @PostMapping("/providers/vllm/test/chat")
    public Map<String, Object> testVllmChat(@RequestBody VllmChatTestRequest request) {
        return vllmGatewayTestService.testChat(request);
    }

    @PostMapping("/providers/vllm/test/embedding")
    public Map<String, Object> testVllmEmbedding(@RequestBody VllmEmbeddingTestRequest request) {
        return vllmGatewayTestService.testEmbedding(request);
    }

    @PostMapping("/providers/vllm/test/rerank")
    public Map<String, Object> testVllmRerank(@RequestBody VllmRerankTestRequest request) {
        return vllmGatewayTestService.testRerank(request);
    }
    @GetMapping("/providers/openai") public DebugApiLlmConnectionInfo openAiStatus() { return discoveryService.inspect("OPENAI", openAiConnectionService.getBaseUrl(), openAiConnectionService.getResolvedApiKey(), openAiConnectionService.getKeyProvider(), openAiConnectionService.getConfiguredOrDefaultModel(), openAiConnectionService.getHealthCheckPath(), openAiConnectionService.getHealthCheckMethod(), openAiConnectionService.getModelsPath(), openAiConnectionService.getModelsMethod()); }
    @PostMapping("/providers/openai") public DebugApiLlmConnectionInfo updateOpenAi(@RequestBody(required = false) DebugApiLlmProviderConfig request) { openAiConnectionService.update(request == null ? null : request.getBaseUrl(), request == null ? null : request.getDefaultModel(), request == null ? null : request.getKeyProvider(), request == null ? null : request.getHealthCheckPath(), request == null ? null : request.getHealthCheckMethod(), request == null ? null : request.getModelsPath(), request == null ? null : request.getModelsMethod()); return openAiStatus(); }
    @PostMapping("/providers/openai/reset") public DebugApiLlmConnectionInfo resetOpenAi() { openAiConnectionService.reset(); return openAiStatus(); }
}
