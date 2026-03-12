package ai.local.nalbbun.service.conversation;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class ConversationIdResolver {

    public static final String SESSION_CONVERSATION_ID_KEY = "APP_CONVERSATION_ID";
    private static final String HEADER_NAME = "X-Conversation-Id";

    public String resolve(HttpServletRequest request, HttpSession session) {
        String headerConversationId = sanitize(request == null ? null : request.getHeader(HEADER_NAME));
        if (!headerConversationId.isBlank()) {
            session.setAttribute(SESSION_CONVERSATION_ID_KEY, headerConversationId);
            return headerConversationId;
        }

        Object stored = session.getAttribute(SESSION_CONVERSATION_ID_KEY);
        if (stored instanceof String existing && !existing.isBlank()) {
            return existing;
        }

        String generated = generate();
        session.setAttribute(SESSION_CONVERSATION_ID_KEY, generated);
        return generated;
    }

    String sanitize(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._:-]", "-")
                .replaceAll("-+", "-");

        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }

        return normalized;
    }

    private String generate() {
        return "conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
