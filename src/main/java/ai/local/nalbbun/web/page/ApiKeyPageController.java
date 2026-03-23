package ai.local.nalbbun.web.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiKeyPageController {

    @GetMapping("/api-keys")
    public String page() {
        return "api-keys";
    }
}
