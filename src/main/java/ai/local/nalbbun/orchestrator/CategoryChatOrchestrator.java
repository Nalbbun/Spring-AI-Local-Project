package ai.local.nalbbun.orchestrator;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.category.common.CategoryResolver;
import ai.local.nalbbun.category.model.CategoryResolution;
import ai.local.nalbbun.category.model.CategoryResult;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.ConversationState;
import ai.local.nalbbun.memory.service.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;

/**
 * Category Chat Orchestrator 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class CategoryChatOrchestrator {

    private final CategoryResolver categoryResolver;
    private final CategoryHandlerRegistry categoryHandlerRegistry;
    private final ConversationMemoryService conversationMemoryService;
    private final AgentEventPublisher agentEventPublisher;

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public CategoryResult execute(String userQuery,
                                  String conversationId,
                                  ChatCategory requestedCategory,
                                  SseEmitter emitter) {

        ConversationState state = new ConversationState();
        state.setConversationId(conversationId);
        state.setUserQuery(userQuery);
        state.setRequestedCategory(requestedCategory);

        CategoryResolution resolution = categoryResolver.resolve(userQuery, requestedCategory);
        state.setResolvedCategory(resolution.getCategory());
        state.getAttributes().put("resolverMode", resolution.getResolverMode());
        state.getAttributes().put("resolverReason", resolution.getReason());
        state.getAttributes().put("resolverConfidence", resolution.getConfidence());

        conversationMemoryService.addUserMessage(conversationId, resolution.getCategory(), userQuery);

        agentEventPublisher.send(
                emitter,
                "CategoryResolver",
                "complete",
                String.format(
                        "category=%s, mode=%s, confidence=%d",
                        resolution.getCategory(),
                        resolution.getResolverMode(),
                        resolution.getConfidence()
                )
        );

        CategoryResult result = categoryHandlerRegistry.get(resolution.getCategory()).handle(state, emitter);
        state.setFinalResponse(result.getFinalResponse());

        return result;
    }
}