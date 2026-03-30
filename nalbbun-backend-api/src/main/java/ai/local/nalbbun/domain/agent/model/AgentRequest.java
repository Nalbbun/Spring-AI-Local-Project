package ai.local.nalbbun.domain.agent.model;

import ai.local.nalbbun.domain.category.model.ChatCategory;

public record AgentRequest(
        String conversationId,
        String userMessage,
        ChatCategory categoryType,
        AgentType agentType
) {}
