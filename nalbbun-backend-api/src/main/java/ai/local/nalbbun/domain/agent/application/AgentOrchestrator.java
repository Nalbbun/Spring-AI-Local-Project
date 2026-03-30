package ai.local.nalbbun.domain.agent.application;

import ai.local.nalbbun.domain.agent.executor.AgentExecutor;
import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentResult;
import ai.local.nalbbun.domain.agent.model.AgentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentOrchestrator {
    private final Map<AgentType, AgentExecutor> executors = new EnumMap<>(AgentType.class);

    public AgentOrchestrator(List<AgentExecutor> executorList) {
        for (AgentExecutor executor : executorList) {
            executors.put(executor.supports(), executor);
        }
    }

    public AgentResult execute(AgentRequest request) {
        AgentExecutor executor = executors.get(request.agentType());
        if (executor == null) {
            return new AgentResult("No agent executor registered for " + request.agentType(), null);
        }
        return executor.execute(request);
    }
}
