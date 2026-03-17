package ai.local.nalbbun.service.conversation;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Conversation Id Resolver 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Component
public class ConversationIdResolver {

    public static final String SESSION_CONVERSATION_ID_KEY = "APP_CONVERSATION_ID";
    private static final String HEADER_NAME = "X-Conversation-Id";

    /**
     * resolve 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String generate() {
        return "conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
