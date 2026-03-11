package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequiredArgsConstructor
public class DebugMemoryController {

    private final ConversationMemoryService conversationMemoryService;

    @GetMapping("/debug/api/memory")
    public ConversationMemorySnapshot memory(HttpSession session) {
        return conversationMemoryService.snapshot(session.getId());
    }

    @PostMapping("/debug/api/memory/clear")
    public String clear(HttpSession session) {
        conversationMemoryService.clear(session.getId());
        return "메모리가 초기화되었습니다.";
    }
}