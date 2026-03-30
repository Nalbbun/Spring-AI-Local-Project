package ai.local.nalbbun.domain.agent.application;

import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentStep;

import java.util.List;

public interface AgentPlanner {
    List<AgentStep> plan(AgentRequest request);
}
