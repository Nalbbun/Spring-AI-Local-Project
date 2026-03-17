package ai.local.nalbbun.category.common;

/**
 * CategoryParserMode는 선택 가능한 상태나 유형을 정의하는 열거형이다.
 * <p>주요 기능: category parser mode 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public enum CategoryParserMode {
    RULE,
    LLM,
    HYBRID
}