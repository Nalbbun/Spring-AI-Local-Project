package ai.local.nalbbun.debug.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("local")
public class DebugHomeController {

    @GetMapping("/debug")
    public String home() {
        return "debug/home";
    }
}