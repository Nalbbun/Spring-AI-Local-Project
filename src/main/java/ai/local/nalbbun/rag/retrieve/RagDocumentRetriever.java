package ai.local.nalbbun.rag.retrieve;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.service.RagMetadataSupport;
import ai.local.nalbbun.rag.service.RuntimeOllamaVectorStoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RagDocumentRetriever는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag document retriever 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagDocumentRetriever {

    /** runtimeVectorStoreFactory 값을 보관한다. */
    private final RuntimeOllamaVectorStoreFactory runtimeVectorStoreFactory;
    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** ragMetadataSupport 값을 보관한다. */
    private final RagMetadataSupport ragMetadataSupport;

    /**
     * retrieve 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param query 사용자 입력 또는 질의 내용
     * @return 조회 또는 생성된 목록
     */
    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query) {
        return retrieve(category, query, null, null);
    }

    /**
     * retrieve 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param query 사용자 입력 또는 질의 내용
     * @param sourceFilter sourceFilter 값
     * @param versionFilter versionFilter 값
     * @return 조회 또는 생성된 목록
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
     * 현재 상태를 다른 표현 형태로 변환한다.
     *
     * @param fallbackCategory fallbackCategory 값
     * @param document document 값
     * @return RagRetrievedDocument 타입의 처리 결과
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
