package ai.local.nalbbun.domain.rag.reader;

/**
 * Rag File Type 타입이다.
 *
 * <p>기능 설명: 외부 소스 또는 파일에서 데이터를 읽는다. 열거형 상수는 상태 표현이나 분기 기준으로 사용된다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public enum RagFileType {
    PDF,
    MARKDOWN,
    TEXT
}
