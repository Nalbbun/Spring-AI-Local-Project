package ai.local.nalbbun.service.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * ConversationIdResolverTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: conversation id resolver test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class ConversationIdResolverTest {

    /** resolver 값을 보관한다. */
    private final ConversationIdResolver resolver = new ConversationIdResolver();

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldUseSanitizedHeaderConversationIdWhenProvided() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.addHeader("X-Conversation-Id", " Team Chat #1 ");

        String conversationId = resolver.resolve(request, session);

        assertEquals("team-chat-1", conversationId);
        assertEquals("team-chat-1", session.getAttribute(ConversationIdResolver.SESSION_CONVERSATION_ID_KEY));
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldReuseSessionConversationIdWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ConversationIdResolver.SESSION_CONVERSATION_ID_KEY, "conv-existing-001");

        String conversationId = resolver.resolve(request, session);

        assertEquals("conv-existing-001", conversationId);
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldGenerateConversationIdWhenNoHeaderAndNoSessionValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        String conversationId = resolver.resolve(request, session);

        assertTrue(conversationId.startsWith("conv-"));
        assertFalse(conversationId.isBlank());
    }
}
