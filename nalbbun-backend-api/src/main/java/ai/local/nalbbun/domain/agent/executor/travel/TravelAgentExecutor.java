package ai.local.nalbbun.domain.agent.executor.travel;

import ai.local.nalbbun.domain.agent.application.travel.TravelWorkflow;
import ai.local.nalbbun.domain.agent.executor.AgentExecutor;
import ai.local.nalbbun.domain.agent.model.AgentRequest;
import ai.local.nalbbun.domain.agent.model.AgentResult;
import ai.local.nalbbun.domain.agent.model.AgentType;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import org.springframework.stereotype.Component;

@Component
public class TravelAgentExecutor implements AgentExecutor {
    private final TravelWorkflow travelWorkflow;

    public TravelAgentExecutor(TravelWorkflow travelWorkflow) {
        this.travelWorkflow = travelWorkflow;
    }

    @Override
    public AgentType supports() {
        return AgentType.TRAVEL;
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        TravelContext context = new TravelContext();
        context.setUserQuery(request.userMessage());
        return new AgentResult("Travel agent execution scaffold", context);
    }
}
