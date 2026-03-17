package ai.local.nalbbun.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatAgentController {

    @GetMapping("/chat/agent")
    public String page() {
        return "chat-agent";
    }
}
