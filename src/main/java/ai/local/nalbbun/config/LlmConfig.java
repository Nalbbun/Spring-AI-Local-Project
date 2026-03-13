package ai.local.nalbbun.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel; 
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LlmConfig {
  // OpenAI ChatClient.Builder 빈 생성
  // @Qualifier("openaiBuilder")로 주입받아 사용
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
  @Bean
  @Qualifier("ollamaBuilder")
  public ChatClient.Builder ollamaChatClientBuilder(OllamaChatModel ollamaChatModel) {
    return ChatClient.builder(ollamaChatModel);
  }

  // 기본 ChatClient.Builder 빈 생성
  // @Qualifier 없이 주입받으면 OpenAI의 ChatClient.Builder 사용
  @Bean
  @Primary
  public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel);
  }
}
