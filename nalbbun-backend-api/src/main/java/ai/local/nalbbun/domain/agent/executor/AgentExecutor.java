package ai.local.nalbbun.domain.agent.executor;

import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentResult;
import ai.local.nalbbun.domain.agent.model.AgentType;

public interface AgentExecutor {
    AgentType supports();
    AgentResult execute(AgentRequest request);
}
