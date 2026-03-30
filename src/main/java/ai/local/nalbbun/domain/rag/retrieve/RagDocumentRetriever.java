package ai.local.nalbbun.domain.rag.retrieve;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.domain.rag.service.RagMetadataSupport;
import ai.local.nalbbun.domain.rag.service.RuntimeOllamaVectorStoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Rag Document Retriever 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagDocumentRetriever {

    private final RuntimeOllamaVectorStoreFactory runtimeVectorStoreFactory;
    private final RagProperties ragProperties;
    private final RagMetadataSupport ragMetadataSupport;

    /**
     * retrieve 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query) {
        return retrieve(category, query, null, null);
    }

    /**
     * retrieve 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query, String sourceFilter, String versionFilter) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        VectorStore vectorStore = runtimeVectorStoreFactory.create();
        if (vectorStore == null) {
            return List.of();
        }

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(ragProperties.getTopK())
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .filterExpression(ragMetadataSupport.buildFilterExpression(category, sourceFilter, versionFilter))
                    .build();

            return vectorStore.similaritySearch(request).stream()
                    .map(document -> toRetrievedDocument(category, document))
                    .toList();
        } catch (Exception e) {
            log.warn("RAG retrieval failed. category={}, source={}, version={}, reason={}", category, sourceFilter, versionFilter, e.getMessage());
            return List.of();
        }
    }

    /**
     * to Retrieved Document 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RagRetrievedDocument toRetrievedDocument(ChatCategory fallbackCategory, Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object rawCategory = metadata.getOrDefault("category", fallbackCategory.name());
        ChatCategory category = ChatCategory.valueOf(String.valueOf(rawCategory));
        return RagRetrievedDocument.builder()
                .id(document.getId())
                .title(String.valueOf(metadata.getOrDefault("title", metadata.getOrDefault("source", "untitled"))))
                .source(String.valueOf(metadata.getOrDefault("source", "manual")))
                .version(String.valueOf(metadata.getOrDefault("version", "v1")))
                .category(category)
                .text(document.getText())
                .score(document.getScore())
                .build();
    }
}
