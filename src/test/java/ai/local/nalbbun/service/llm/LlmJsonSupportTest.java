package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LlmJsonSupportTest {

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

    @Test
    void shouldExtractJsonArrayFromWrappedExplanation() {
        String raw = "결과는 다음과 같습니다: [{\"name\":\"A\"},{\"name\":\"B\"}] 감사합니다.";

        assertEquals("[{\"name\":\"A\"},{\"name\":\"B\"}]", LlmJsonSupport.extractArray(raw));
    }
}
