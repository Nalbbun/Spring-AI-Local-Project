package ai.local.nalbbun.service.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import ai.local.nalbbun.conversation.ConversationIdResolver;

/**
 * Conversation Id Resolver Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class ConversationIdResolverTest {

    private final ConversationIdResolver resolver = new ConversationIdResolver();

    /**
     * Use Sanitized Header Conversation Id When Provided 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Reuse Session Conversation Id When Header Missing 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Generate Conversation Id When No Header And No Session Value 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
