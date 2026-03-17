package ai.local.nalbbun.debug.controller;

import ai.local.nalbbun.port.WebSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DebugSearchController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug search controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DebugSearchController {

    /** webSearchPort 값을 보관한다. */
    private final WebSearchPort webSearchPort;

    /**
     * 대상 정보를 조회한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/debug/api/search")
    public Map<String, Object> search(@RequestParam(name = "query") String query) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", webSearchPort.providerName());
        response.put("query", query);
        response.put("result", webSearchPort.search(query));
        return response;
    }

    /**
     * fetch 기능을 수행한다.
     *
     * @param url 대상 URL
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/debug/api/search/fetch")
    public Map<String, Object> fetch(@RequestParam(name = "url") String url) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", webSearchPort.providerName());
        response.put("url", url);
        response.put("result", webSearchPort.fetch(url));
        return response;
    }
}
