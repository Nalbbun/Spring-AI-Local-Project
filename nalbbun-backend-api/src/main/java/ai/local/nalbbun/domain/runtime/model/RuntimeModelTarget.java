package ai.local.nalbbun.domain.runtime.model;

/**
 * Runtime Model Target 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 열거형 상수는 상태 표현이나 분기 기준으로 사용된다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
public enum RuntimeModelTarget {
    GENERAL,
    DEV,
    MICE,
    TRAVEL_SEARCH,
    TRAVEL_PLAN
}