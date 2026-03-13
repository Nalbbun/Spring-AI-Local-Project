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

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DebugMemoryController {

    private final ConversationMemoryService conversationMemoryService;
    private final ConversationIdResolver conversationIdResolver;

    @GetMapping("/debug/api/memory")
    public ConversationMemorySnapshot memory(HttpServletRequest request, HttpSession session) {
        return conversationMemoryService.snapshot(conversationIdResolver.resolve(request, session));
    }

    @PostMapping("/debug/api/memory/clear")
    public String clear(HttpServletRequest request, HttpSession session) {
        conversationMemoryService.clear(conversationIdResolver.resolve(request, session));
        return "메모리가 초기화되었습니다.";
    }
}
