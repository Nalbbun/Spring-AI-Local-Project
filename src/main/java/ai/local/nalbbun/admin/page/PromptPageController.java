package ai.local.nalbbun.admin.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromptPageController {

    @GetMapping("/prompts")
    public String page() {
        return "prompts";
    }
}
