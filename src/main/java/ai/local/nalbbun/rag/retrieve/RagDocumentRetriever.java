package ai.local.nalbbun.rag.retrieve;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagDocumentRetriever {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final RagMetadataSupport ragMetadataSupport;

    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query) {
        return retrieve(category, query, null, null);
    }

    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query, String sourceFilter, String versionFilter) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null || query == null || query.isBlank()) {
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
