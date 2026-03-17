package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.category.model.CategoryContext;
import ai.local.nalbbun.category.model.ConversationState;

/**
 * Category Parsing Strategy 인터페이스이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 구현체가 따라야 할 계약을 정의한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public interface CategoryParsingStrategy<T extends CategoryContext> {

    T parse(ConversationState state, T context);

    String mode();
}