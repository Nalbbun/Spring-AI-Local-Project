package ai.local.nalbbun.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel; 
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LlmConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: llm config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Configuration
public class LlmConfig {
  // OpenAI ChatClient.Builder 빈 생성
  // @Qualifier("openaiBuilder")로 주입받아 사용
  /**
   * openaiChatClientBuilder 기능을 수행한다.
   *
   * @param openAiChatModel openAiChatModel 값
   * @return ChatClient.Builder 타입의 처리 결과
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
   * ollamaChatClientBuilder 기능을 수행한다.
   *
   * @param ollamaChatModel ollamaChatModel 값
   * @return ChatClient.Builder 타입의 처리 결과
   */
  @Bean
  @Qualifier("ollamaBuilder")
  public ChatClient.Builder ollamaChatClientBuilder(OllamaChatModel ollamaChatModel) {
    return ChatClient.builder(ollamaChatModel);
  }

  // 기본 ChatClient.Builder 빈 생성
  // @Qualifier 없이 주입받으면 OpenAI의 ChatClient.Builder 사용
  /**
   * chatClientBuilder 기능을 수행한다.
   *
   * @param openAiChatModel openAiChatModel 값
   * @return ChatClient.Builder 타입의 처리 결과
   */
  @Bean
  @Primary
  public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel);
  }
}
