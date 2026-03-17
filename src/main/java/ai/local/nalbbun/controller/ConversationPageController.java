package ai.local.nalbbun.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConversationPageController {

    @GetMapping("/memory")
    public String page() {
        return "memory";
    }
}
