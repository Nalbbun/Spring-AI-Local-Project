package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * CategoryHandler는 구현체가 따라야 할 동작 계약을 정의하는 인터페이스이다.
 * <p>주요 기능: category handler 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public interface CategoryHandler {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    ChatCategory category();

    /**
     * 지원 여부를 확인한다.
     *
     * @param category 대상 카테고리 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    default boolean supports(ChatCategory category) {
        return this.category() == category;
    }

    /**
     * 요청 또는 상태를 처리한다.
     *
     * @param state 현재 처리 상태 정보
     * @param emitter SSE 이벤트 전송 객체
     * @return CategoryResult 타입의 처리 결과
     */
    CategoryResult handle(ConversationState state, SseEmitter emitter);
}