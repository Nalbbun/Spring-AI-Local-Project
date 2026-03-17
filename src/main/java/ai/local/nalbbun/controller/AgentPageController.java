package ai.local.nalbbun.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AgentPageController {

    @GetMapping("/agent")
    public String page() {
        return "agent";
    }
}
