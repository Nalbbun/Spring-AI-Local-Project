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
 * Debug Runtime Config Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/config")
public class DebugRuntimeConfigController {

    private final DebugRuntimeConfigService debugRuntimeConfigService;
    private final ConversationIdResolver conversationIdResolver;

    /**
     * get Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping
    public DebugRuntimeConfig getConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.getCurrentConfig(), request, session);
    }

    /**
     * update Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * reset Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/reset")
    public DebugRuntimeConfig resetConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.reset(), request, session);
    }

    /**
     * with Conversation Id 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
