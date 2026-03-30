package ai.local.nalbbun.admin.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Chat Agent Controller 타입이다.
 *
 * <p>기능 설명: HTTP 요청을 받아 서비스 또는 오케스트레이터로 전달하고 응답을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: HTTP 요청 파라미터, 요청 본문, 세션 또는 헤더 정보</p>
 * <p>출력: HTTP 응답, SSE 이벤트, 뷰 이름 또는 직렬화 가능한 결과</p>
 */
@Controller
public class ChatAgentController {

    /**
     * page 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/chat/agent")
    public String page() {
        return "chat-agent";
    }
}
