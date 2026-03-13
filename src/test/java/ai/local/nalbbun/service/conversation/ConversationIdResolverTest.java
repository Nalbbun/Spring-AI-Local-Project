package ai.local.nalbbun.service.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class ConversationIdResolverTest {

    private final ConversationIdResolver resolver = new ConversationIdResolver();

    @Test
    void shouldUseSanitizedHeaderConversationIdWhenProvided() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.addHeader("X-Conversation-Id", " Team Chat #1 ");

        String conversationId = resolver.resolve(request, session);

        assertEquals("team-chat-1", conversationId);
        assertEquals("team-chat-1", session.getAttribute(ConversationIdResolver.SESSION_CONVERSATION_ID_KEY));
    }

    @Test
    void shouldReuseSessionConversationIdWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ConversationIdResolver.SESSION_CONVERSATION_ID_KEY, "conv-existing-001");

        String conversationId = resolver.resolve(request, session);

        assertEquals("conv-existing-001", conversationId);
    }

    @Test
    void shouldGenerateConversationIdWhenNoHeaderAndNoSessionValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        String conversationId = resolver.resolve(request, session);

        assertTrue(conversationId.startsWith("conv-"));
        assertFalse(conversationId.isBlank());
    }
}
