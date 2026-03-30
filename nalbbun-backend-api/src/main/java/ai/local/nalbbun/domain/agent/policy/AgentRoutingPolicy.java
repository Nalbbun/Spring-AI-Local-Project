package ai.local.nalbbun.domain.agent.policy;

import ai.local.nalbbun.domain.agent.model.AgentType;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import org.springframework.stereotype.Component;

@Component
public class AgentRoutingPolicy {
    public boolean shouldUseAgent(ChatCategory categoryType, String message) {
        if (categoryType == ChatCategory.TRAVEL) return true;
        String normalized = message == null ? "" : message.toLowerCase();
        return normalized.contains("단계") || normalized.contains("분석") || normalized.contains("계획");
    }

    public AgentType resolveAgentType(ChatCategory categoryType) {
        return switch (categoryType) {
            case TRAVEL -> AgentType.TRAVEL;
            case DEV -> AgentType.DEV;
            case MICE -> AgentType.MICE;
            default -> AgentType.GENERAL_ASSIST;
        };
    }
}
