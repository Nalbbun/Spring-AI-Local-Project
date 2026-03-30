package ai.local.nalbbun.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiCatalogPageController {

    @GetMapping({"/", "/api-docs"})
    public String apiDocsPage() {
        return "api-docs";
    }
}
