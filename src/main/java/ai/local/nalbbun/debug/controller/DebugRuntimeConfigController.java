package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.service.conversation.ConversationIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * DebugRuntimeConfigController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug runtime config controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/config")
public class DebugRuntimeConfigController {

    /** debugRuntimeConfigService 값을 보관한다. */
    private final DebugRuntimeConfigService debugRuntimeConfigService;
    /** conversationIdResolver 값을 보관한다. */
    private final ConversationIdResolver conversationIdResolver;

    /**
     * 지정된 정보를 조회한다.
     *
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
    @GetMapping
    public DebugRuntimeConfig getConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.getCurrentConfig(), request, session);
    }

    /**
     * 대상 값을 갱신한다.
     *
     * @param request HTTP 요청 객체
     * @param httpRequest HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
    @PostMapping
    public DebugRuntimeConfig updateConfig(
            @RequestBody DebugRuntimeConfig request,
            HttpServletRequest httpRequest,
            HttpSession session
    ) {
        return withConversationId(debugRuntimeConfigService.update(request), httpRequest, session);
    }

    /**
     * resetConfig 기능을 수행한다.
     *
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
    @PostMapping("/reset")
    public DebugRuntimeConfig resetConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.reset(), request, session);
    }

    /**
     * withConversationId 기능을 수행한다.
     *
     * @param config 설정 정보
     * @param request HTTP 요청 객체
     * @param session HTTP 세션 객체
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
    private DebugRuntimeConfig withConversationId(
            DebugRuntimeConfig config,
            HttpServletRequest request,
            HttpSession session
    ) {
        config.setConversationId(conversationIdResolver.resolve(request, session));
        return config;
    }
}
