package ai.local.nalbbun.rag.retrieve;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.service.RagMetadataSupport;
import ai.local.nalbbun.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagDocumentRetriever {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final RagMetadataSupport ragMetadataSupport;
    private final ObjectProvider<DebugRagTraceService> debugRagTraceServiceProvider;

    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query) {
        return retrieve(category, query, null, null, null);
    }

    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query, String sourceFilter, String versionFilter) {
        return retrieve(category, query, sourceFilter, versionFilter, null);
    }

    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query, String sourceFilter, String versionFilter, String traceId) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null || query == null || query.isBlank()) {
            trace(traceId, "RETRIEVE", "SKIP", "리트리버 실행 조건 불충분", Map.of(
                    "vectorStoreAvailable", vectorStore != null,
                    "hasQuery", query != null && !query.isBlank()
            ));
            return List.of();
        }

        try {
            String filterExpression = ragMetadataSupport.buildFilterExpression(category, sourceFilter, versionFilter);
            trace(traceId, "RETRIEVE", "REQUEST_BUILD", "SearchRequest 구성", Map.of(
                    "category", category.name(),
                    "topK", ragProperties.getTopK(),
                    "similarityThreshold", ragProperties.getSimilarityThreshold(),
                    "sourceFilter", safeValue(sourceFilter),
                    "versionFilter", safeValue(versionFilter),
                    "filterExpression", safeValue(filterExpression)
            ));

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(ragProperties.getTopK())
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .filterExpression(filterExpression)
                    .build();

            List<Document> searched = vectorStore.similaritySearch(request);
            trace(traceId, "RETRIEVE", "SIMILARITY_SEARCH_COMPLETE", "유사도 검색 완료", Map.of(
                    "hitCount", searched.size()
            ));

            return searched.stream()
                    .map(document -> toRetrievedDocument(category, document))
                    .toList();
        } catch (Exception e) {
            log.warn("RAG retrieval failed. category={}, source={}, version={}, reason={}", category, sourceFilter, versionFilter, e.getMessage());
            error(traceId, "RETRIEVE", "FAILED", "리트리버 실행 실패", Map.of(
                    "reason", e.getMessage(),
                    "category", category.name()
            ));
            return List.of();
        }
    }

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

    private void trace(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.info(traceId, operation, stage, message, new LinkedHashMap<>(details));
        }
    }

    private void error(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.error(traceId, operation, stage, message, new LinkedHashMap<>(details));
        }
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
