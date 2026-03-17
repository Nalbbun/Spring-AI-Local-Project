package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Llm Json Support Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class LlmJsonSupportTest {

    /**
     * Extract Json Object From Markdown Response 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldExtractJsonObjectFromMarkdownResponse() {
        String raw = """
                ```json
                {"category":"DEV","confidence":91,"reason":"code request"}
                ```
                """;

        assertEquals("{" + "\"category\":\"DEV\",\"confidence\":91,\"reason\":\"code request\"}",
                LlmJsonSupport.extractObject(raw));
    }

    /**
     * Extract Json Array From Wrapped Explanation 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldExtractJsonArrayFromWrappedExplanation() {
        String raw = "결과는 다음과 같습니다: [{\"name\":\"A\"},{\"name\":\"B\"}] 감사합니다.";

        assertEquals("[{\"name\":\"A\"},{\"name\":\"B\"}]", LlmJsonSupport.extractArray(raw));
    }
}
