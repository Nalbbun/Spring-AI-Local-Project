package ai.local.nalbbun.admin.api;

import ai.local.nalbbun.domain.search.port.WebSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug Search Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DebugSearchController {

    private final WebSearchPort webSearchPort;

    /**
     * search 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * fetch 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
