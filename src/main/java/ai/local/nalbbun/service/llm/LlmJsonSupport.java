package ai.local.nalbbun.service.llm;

public final class LlmJsonSupport {

    private LlmJsonSupport() {
    }

    public static String extractObject(String raw) {
        return extractBalanced(raw, '{', '}', "{}");
    }

    public static String extractArray(String raw) {
        return extractBalanced(raw, '[', ']', "[]");
    }

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

    private static String stripCodeFence(String raw) {
        String text = raw;
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("```\\s*$", "");
        }
        return text.trim();
    }
}
