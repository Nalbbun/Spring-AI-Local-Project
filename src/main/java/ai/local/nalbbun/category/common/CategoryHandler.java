package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Category Handler 인터페이스이다.
 *
 * <p>기능 설명: 카테고리 또는 기능별 요청 처리 진입점을 담당한다. 구현체가 따라야 할 계약을 정의한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public interface CategoryHandler {

    ChatCategory category();

    /**
     * supports 가능 여부를 확인한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    default boolean supports(ChatCategory category) {
        return this.category() == category;
    }

    CategoryResult handle(ConversationState state, SseEmitter emitter);
}