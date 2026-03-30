package ai.local.nalbbun.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel; 
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Llm Config 타입이다.
 *
 * <p>기능 설명: 스프링 빈과 런타임 설정을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 프로퍼티 값, 환경 변수, 스프링 컨텍스트 정보</p>
 * <p>출력: 빈 등록 결과 또는 런타임 설정 상태</p>
 */
@Configuration
public class LlmConfig {
  // OpenAI ChatClient.Builder 빈 생성
  // @Qualifier("openaiBuilder")로 주입받아 사용
  /**
   * openai Chat Client Builder 기능을 수행한다.
   *
   * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
   * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
   */
  @Bean
  @Qualifier("openaiBuilder")
  public ChatClient.Builder openaiChatClientBuilder(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel);
  }

  // Google Gemini ChatClient.Builder 빈 생성
  // @Qualifier("geminiBuilder")로 주입받아 사용
//  @Bean
//  @Qualifier("geminiBuilder")
//  public ChatClient.Builder geminiChatClientBuilder(VertexAiGeminiChatModel geminiChatModel) {
//    return ChatClient.builder(geminiChatModel);
//  }

  // Ollama ChatClient.Builder 빈 생성
  // @Qualifier("ollamaBuilder")로 주입받아 사용
  /**
   * ollama Chat Client Builder 기능을 수행한다.
   *
   * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
   * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
   */
  @Bean
  @Qualifier("ollamaBuilder")
  public ChatClient.Builder ollamaChatClientBuilder(OllamaChatModel ollamaChatModel) {
    return ChatClient.builder(ollamaChatModel);
  }

  // 기본 ChatClient.Builder 빈 생성
  // @Qualifier 없이 주입받으면 OpenAI의 ChatClient.Builder 사용
  /**
   * chat Client Builder 기능을 수행한다.
   *
   * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
   * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
   */
  @Bean
  @Primary
  public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel);
  }
}
