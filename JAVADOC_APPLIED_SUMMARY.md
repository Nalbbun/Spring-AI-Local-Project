# Javadoc 적용 요약

- 수정된 Java 파일 수: 170
- 적용 범위: 클래스/인터페이스/enum/record 설명, 메서드/생성자 @param @return, 필드 설명
- build.gradle에 UTF-8 기반 Javadoc 생성 설정 추가

## 수정 파일 일부
- src/test/java/ai/local/nalbbun/NalbbunAiLocalApplicationTests.java (2개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/service/conversation/ConversationIdResolverTest.java (5개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/service/llm/LlmJsonSupportTest.java (3개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/service/llm/RuntimeModelResolverTest.java (5개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/service/memory/InMemoryConversationMemoryServiceTest.java (4개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/service/memory/JdbcConversationMemoryServiceTest.java (6개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/rag/service/RagMetadataSupportTest.java (4개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/rag/service/RagPromptComposerTest.java (2개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/rag/service/RagSupportServiceTest.java (3개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/rag/eval/service/RagEvaluationServiceTest.java (2개 주석 블록 추가)
- src/test/java/ai/local/nalbbun/debug/service/DebugRuntimeConfigServiceTest.java (3개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/NalbbunAiLocalApplication.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/config/AsyncExecutionConfig.java (5개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/config/Jackson3Config.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/config/JdbcMemoryDataSourceConfig.java (4개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/config/LlmConfig.java (4개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/config/WebEncodingConfig.java (3개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/controller/HomeController.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/controller/RuntimeInfoController.java (3개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/orchestrator/CategoryChatOrchestrator.java (6개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/port/WebSearchPort.java (4개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/registry/CategoryHandlerRegistry.java (4개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/support/sse/AgentEventPublisher.java (3개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/support/sse/SseEmitterHelper.java (6개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/support/sse/SseEventNames.java (6개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/conversation/ConversationIdResolver.java (6개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/ExternalLlmFallbackPolicy.java (3개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/LlmJsonSupport.java (6개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/RuntimeModelChatService.java (23개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/RuntimeModelResolutionException.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/RuntimeModelResolver.java (9개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/llm/RuntimeModelSelection.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/memory/ConversationMemoryService.java (12개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/memory/InMemoryConversationMemoryService.java (18개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/memory/JdbcConversationMemoryService.java (26개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/memory/RedisConversationMemoryService.java (26개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/prompt/PromptMemoryContextBuilder.java (5개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/search/DummyWebSearchService.java (10개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/service/search/TavilyWebSearchService.java (11개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/config/RagProperties.java (27개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/controller/DebugRagController.java (28개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagDocumentIngestionService.java (20개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagFileIngestionItemResult.java (2개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagIngestCommand.java (7개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagIngestionResult.java (1개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagMultiFileIngestionResult.java (1개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/ingest/RagUrlIngestCommand.java (7개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/model/RagContext.java (11개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/model/RagRetrievedDocument.java (1개 주석 블록 추가)
- src/main/java/ai/local/nalbbun/rag/model/RagSourceFileEntry.java (19개 주석 블록 추가)

## 생성 명령 예시
```bash
./gradlew clean javadoc
```
