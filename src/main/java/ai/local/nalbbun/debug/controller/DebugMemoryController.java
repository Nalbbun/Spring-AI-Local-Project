package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.service.conversation.ConversationIdResolver;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DebugMemoryController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug memory controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DebugMemoryController {

    /** conversationMemoryService 값을 보관한다. */
    private final ConversationMemoryService conversationMemoryService;
    /** conversationIdResolver 값을 보관한다. */
    private final ConversationIdResolver conversationIdResolver;

    /**
     * memory 기능을 수행한다.
     *
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return ConversationMemorySnapshot 타입의 처리 결과
     */
    @GetMapping("/debug/api/memory")
    public ConversationMemorySnapshot memory(HttpServletRequest request, HttpSession session) {
        return conversationMemoryService.snapshot(conversationIdResolver.resolve(request, session));
    }

    /**
     * clear 기능을 수행한다.
     *
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return 처리 결과 문자열
     */
    @PostMapping("/debug/api/memory/clear")
    public String clear(HttpServletRequest request, HttpSession session) {
        conversationMemoryService.clear(conversationIdResolver.resolve(request, session));
        return "메모리가 초기화되었습니다.";
    }
}
