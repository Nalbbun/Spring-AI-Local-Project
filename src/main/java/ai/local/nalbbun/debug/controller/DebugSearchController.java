package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.port.WebSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("local")
@RequiredArgsConstructor
public class DebugSearchController {

    private final WebSearchPort webSearchPort;

    @GetMapping("/debug/api/search")
    public Map<String, Object> search(@RequestParam(name = "query") String query) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "dummy");
        response.put("query", query);
        response.put("result", webSearchPort.search(query));
        return response;
    }

    @GetMapping("/debug/api/search/fetch")
    public Map<String, Object> fetch(@RequestParam(name = "url") String url) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "dummy");
        response.put("url", url);
        response.put("result", webSearchPort.fetch(url));
        return response;
    }
}