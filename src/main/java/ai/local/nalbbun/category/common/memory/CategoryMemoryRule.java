package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * CategoryMemoryRule는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: category memory rule 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public interface CategoryMemoryRule<T extends CategoryContext> {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    ChatCategory category();

    /**
     * extract 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @param assistantResponse assistantResponse 값
     * @return CategoryMemoryUpdate 타입의 처리 결과
     */
    CategoryMemoryUpdate extract(ConversationState state, T context, String assistantResponse);
}