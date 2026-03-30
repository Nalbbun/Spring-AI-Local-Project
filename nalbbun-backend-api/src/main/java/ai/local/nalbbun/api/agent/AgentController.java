package ai.local.nalbbun.api.agent;

import ai.local.nalbbun.domain.agent.application.AgentOrchestrator;
import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentResult;
import ai.local.nalbbun.domain.agent.model.AgentType;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentOrchestrator agentOrchestrator;

    public AgentController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostMapping("/execute")
    public AgentResult execute(@RequestBody AgentRequestBody request) {
        return agentOrchestrator.execute(new AgentRequest(
                request.conversationId(),
                request.userMessage(),
                request.categoryType(),
                request.agentType() == null ? AgentType.GENERAL_ASSIST : request.agentType()
        ));
    }

    public record AgentRequestBody(String conversationId, String userMessage, ChatCategory categoryType, AgentType agentType) {}
}
