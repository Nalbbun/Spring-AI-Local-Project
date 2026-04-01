package ai.local.nalbbun.api.agent;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.api.dto.agent.AgentExecutionResponseDto;
import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.domain.agent.application.AgentOrchestrator;
import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentType;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.search.port.WebSearchPort;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService; 

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentOrchestrator agentOrchestrator;
    private final WebSearchPort webSearchPort;
    private final ApiKeyService apiKeyService;
    private final Environment environment;

    public AgentController(AgentOrchestrator agentOrchestrator, WebSearchPort webSearchPort, ApiKeyService apiKeyService, Environment environment) {
        this.agentOrchestrator = agentOrchestrator;
        this.webSearchPort = webSearchPort;
        this.apiKeyService = apiKeyService;
        this.environment = environment;
    }

    @GetMapping("/web-search-status")
    public ApiResponse<java.util.Map<String, Object>> webSearchStatus() {
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        java.util.Map<String, Object> runtimeStatus = apiKeyService.runtimeStatus();
        boolean localProfile = java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "local".equalsIgnoreCase(profile));
        boolean debugEnabled = environment.getProperty("app.debug.enabled", Boolean.class, false);
        String provider = webSearchPort.providerName();
        response.put("provider", provider);
        response.put("primaryEndpoint", "/api/agent/web-search-test");
        response.put("primaryEndpointAvailable", true);
        response.put("legacyDebugEndpoint", "/debug/api/search");
        response.put("legacyDebugEndpointAvailable", localProfile && debugEnabled);
        response.put("localProfile", localProfile);
        response.put("debugEnabled", debugEnabled);
        response.put("activeProfiles", java.util.Arrays.asList(environment.getActiveProfiles()));
        response.put("tavilyRuntimeStatus", runtimeStatus.get("tavily"));
        response.put("openAiRuntimeStatus", runtimeStatus.get("openai"));
        response.put("hasTavilyActiveKey", !String.valueOf(runtimeStatus.getOrDefault("tavily", "미설정")).contains("미설정"));
        response.put("status", (("tavily".equalsIgnoreCase(provider) && !String.valueOf(runtimeStatus.getOrDefault("tavily", "미설정")).contains("미설정"))
                || !"tavily".equalsIgnoreCase(provider)) ? "ready" : "check-key");
        response.put("message", ("tavily".equalsIgnoreCase(provider) && String.valueOf(runtimeStatus.getOrDefault("tavily", "미설정")).contains("미설정"))
                ? "Tavily provider가 선택되어 있지만 활성 키가 보이지 않습니다. 키 등록/활성 상태와 서버 반영 여부를 확인하세요."
                : "웹 검색 테스트를 실행할 수 있는 상태입니다.");
        return ApiResponse.ok(response);
    }

    @GetMapping("/web-search-test")
    public ApiResponse<java.util.Map<String, Object>> webSearchTest(@RequestParam(name = "query") String query) {
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("provider", webSearchPort.providerName());
        response.put("query", query);
        response.put("result", webSearchPort.search(query));
        return ApiResponse.ok(response);
    }

    @PostMapping("/execute")
    public ApiResponse<AgentExecutionResponseDto> execute(@RequestBody AgentRequestBody request) {
        return ApiResponse.ok(new AgentExecutionResponseDto(agentOrchestrator.execute(new AgentRequest(
                request.conversationId(),
                request.userMessage(),
                request.categoryType(),
                request.agentType() == null ? AgentType.GENERAL_ASSIST : request.agentType()
        ))));
    }

    public record AgentRequestBody(String conversationId, String userMessage, ChatCategory categoryType, AgentType agentType) {}
}
