package ai.local.nalbbun.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatRagController {

    @GetMapping("/chat/rag")
    public String page() {
        return "chat-rag";
    }
}
