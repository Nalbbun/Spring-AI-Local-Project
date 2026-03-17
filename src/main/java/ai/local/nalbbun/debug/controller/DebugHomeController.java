package ai.local.nalbbun.debug.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * DebugHomeController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug home controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Controller
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
public class DebugHomeController {

    /**
     * home 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @GetMapping("/debug")
    public String home() {
        return "debug/home";
    }
}