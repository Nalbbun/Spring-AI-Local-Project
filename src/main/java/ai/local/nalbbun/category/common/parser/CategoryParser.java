package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.CategoryContext;
import ai.local.nalbbun.category.model.ConversationState;

/**
 * Category Parser 인터페이스이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 구현체가 따라야 할 계약을 정의한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public interface CategoryParser<T extends CategoryContext> {
    ChatCategory category();
    T parse(ConversationState state);
}