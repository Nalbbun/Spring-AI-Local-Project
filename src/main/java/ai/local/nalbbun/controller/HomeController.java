package ai.local.nalbbun.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: home controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Controller
public class HomeController {
  /**
   * home 기능을 수행한다.
   * @return 처리 결과 문자열
   */
  @GetMapping("/")
  public String home() {
    return "index";
  } 
}
