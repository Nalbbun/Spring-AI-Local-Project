package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * LlmJsonSupportTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: llm json support test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class LlmJsonSupportTest {

    /**
     * 대상 기능의 동작을 검증한다.
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
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldExtractJsonArrayFromWrappedExplanation() {
        String raw = "결과는 다음과 같습니다: [{\"name\":\"A\"},{\"name\":\"B\"}] 감사합니다.";

        assertEquals("[{\"name\":\"A\"},{\"name\":\"B\"}]", LlmJsonSupport.extractArray(raw));
    }
}
