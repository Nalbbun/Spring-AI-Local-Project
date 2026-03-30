package ai.local.nalbbun.domain.agent.application;

import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentResult;

public interface AgentExecutionService {
    AgentResult execute(AgentRequest request);
}
