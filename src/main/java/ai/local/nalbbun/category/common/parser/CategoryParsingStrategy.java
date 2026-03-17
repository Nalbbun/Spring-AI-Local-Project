package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * CategoryParsingStrategy는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: category parsing strategy 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public interface CategoryParsingStrategy<T extends CategoryContext> {

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return T 타입의 처리 결과
     */
    T parse(ConversationState state, T context);

    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    String mode();
}