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

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/config")
public class DebugRuntimeConfigController {

    private final DebugRuntimeConfigService debugRuntimeConfigService;
    private final ConversationIdResolver conversationIdResolver;

    @GetMapping
    public DebugRuntimeConfig getConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.getCurrentConfig(), request, session);
    }

    @PostMapping
    public DebugRuntimeConfig updateConfig(
            @RequestBody DebugRuntimeConfig request,
            HttpServletRequest httpRequest,
            HttpSession session
    ) {
        return withConversationId(debugRuntimeConfigService.update(request), httpRequest, session);
    }

    @PostMapping("/reset")
    public DebugRuntimeConfig resetConfig(HttpServletRequest request, HttpSession session) {
        return withConversationId(debugRuntimeConfigService.reset(), request, session);
    }

    private DebugRuntimeConfig withConversationId(
            DebugRuntimeConfig config,
            HttpServletRequest request,
            HttpSession session
    ) {
        config.setConversationId(conversationIdResolver.resolve(request, session));
        return config;
    }
}
