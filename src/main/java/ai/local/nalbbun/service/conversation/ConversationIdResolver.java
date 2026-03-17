package ai.local.nalbbun.service.conversation;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * ConversationIdResolver는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: conversation id resolver 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class ConversationIdResolver {

    /** SESSION_CONVERSATION_ID_KEY 값을 보관한다. */
    public static final String SESSION_CONVERSATION_ID_KEY = "APP_CONVERSATION_ID";
    /** HEADER_NAME 값을 보관한다. */
    private static final String HEADER_NAME = "X-Conversation-Id";

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return 처리 결과 문자열
     */
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

    /**
     * sanitize 기능을 수행한다.
     *
     * @param raw raw 값
     * @return 처리 결과 문자열
     */
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

    /**
     * generate 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    private String generate() {
        return "conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
