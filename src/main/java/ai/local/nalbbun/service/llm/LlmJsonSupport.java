package ai.local.nalbbun.service.llm;

/**
 * LlmJsonSupport는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: llm json support 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public final class LlmJsonSupport {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     */
    private LlmJsonSupport() {
    }

    /**
     * extractObject 기능을 수행한다.
     *
     * @param raw raw 값
     * @return 처리 결과 문자열
     */
    public static String extractObject(String raw) {
        return extractBalanced(raw, '{', '}', "{}");
    }

    /**
     * extractArray 기능을 수행한다.
     *
     * @param raw raw 값
     * @return 처리 결과 문자열
     */
    public static String extractArray(String raw) {
        return extractBalanced(raw, '[', ']', "[]");
    }

    /**
     * extractBalanced 기능을 수행한다.
     *
     * @param raw raw 값
     * @param open open 값
     * @param close close 값
     * @param fallback fallback 값
     * @return 처리 결과 문자열
     */
    private static String extractBalanced(String raw, char open, char close, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        String text = stripCodeFence(raw.trim());
        int start = text.indexOf(open);
        if (start < 0) {
            return fallback;
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1).trim();
                }
            }
        }

        return fallback;
    }

    /**
     * stripCodeFence 기능을 수행한다.
     *
     * @param raw raw 값
     * @return 처리 결과 문자열
     */
    private static String stripCodeFence(String raw) {
        String text = raw;
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("```\\s*$", "");
        }
        return text.trim();
    }
}
