package ai.local.nalbbun.domain.rag.retrieve;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.model.RagRetrievalResult;
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
        return retrieveDetailed(category, query, null, null).documents();
    }

    /**
     * retrieve 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagRetrievedDocument> retrieve(ChatCategory category, String query, String sourceFilter, String versionFilter) {
        return retrieveDetailed(category, query, sourceFilter, versionFilter).documents();
    }

    public RagRetrievalResult retrieveDetailed(ChatCategory category, String query, String sourceFilter, String versionFilter) {
        long startedAt = System.currentTimeMillis();
        String filterExpression = ragMetadataSupport.buildFilterExpression(category, sourceFilter, versionFilter);
        if (query == null || query.isBlank()) {
            return RagRetrievalResult.empty(filterExpression, ragProperties.getSimilarityThreshold(), ragProperties.getTopK(), 0L, "blank-query");
        }

        VectorStore vectorStore = runtimeVectorStoreFactory.create();
        if (vectorStore == null) {
            return RagRetrievalResult.empty(filterExpression, ragProperties.getSimilarityThreshold(), ragProperties.getTopK(), 0L, "vector-store-unavailable");
        }

        try {
            int finalTopK = Math.max(1, ragProperties.getTopK());
            int candidateTopK = Math.max(finalTopK * 3, 12);
            double baseThreshold = ragProperties.getSimilarityThreshold();
            double candidateThreshold = Math.max(0.0d, baseThreshold - 0.10d);

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(candidateTopK)
                    .similarityThreshold(candidateThreshold)
                    .filterExpression(filterExpression)
                    .build();

            List<RagRetrievedDocument> candidates = vectorStore.similaritySearch(request).stream()
                    .map(document -> toRetrievedDocument(category, document))
                    .toList();

            boolean rerankApplied = candidates.size() > finalTopK;
            List<RagRetrievedDocument> ranked = rerank(query, candidates).stream()
                    .limit(finalTopK)
                    .toList();

            long elapsedMs = System.currentTimeMillis() - startedAt;
            return new RagRetrievalResult(
                    ranked,
                    candidates.size(),
                    ranked.size(),
                    elapsedMs,
                    filterExpression,
                    candidateThreshold,
                    finalTopK,
                    rerankApplied,
                    "vector-similarity+rereank-title"
            );
        } catch (Exception e) {
            log.warn("RAG retrieval failed. category={}, source={}, version={}, reason={}", category, sourceFilter, versionFilter, e.getMessage());
            return RagRetrievalResult.empty(filterExpression, ragProperties.getSimilarityThreshold(), ragProperties.getTopK(), System.currentTimeMillis() - startedAt, "failed");
        }
    }

    /**
     * to Retrieved Document 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */

    private List<RagRetrievedDocument> rerank(String query, List<RagRetrievedDocument> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> queryTokens = tokenize(query);
        List<ScoredDocument> rescored = new ArrayList<>();
        for (RagRetrievedDocument candidate : candidates) {
            double base = candidate.score() != null ? candidate.score() : 0.0d;
            double lexicalBoost = lexicalBoost(query, queryTokens, candidate);
            rescored.add(new ScoredDocument(candidate, base + lexicalBoost));
        }

        rescored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed()
                .thenComparing(item -> item.document().title(), Comparator.nullsLast(String::compareToIgnoreCase)));

        return rescored.stream().map(ScoredDocument::document).toList();
    }

    private double lexicalBoost(String query, List<String> queryTokens, RagRetrievedDocument candidate) {
        String normalizedQuery = normalize(query);
        String title = normalize(candidate.title());
        String source = normalize(candidate.source());
        String version = normalize(candidate.version());
        String text = normalize(candidate.text());

        double boost = 0.0d;
        if (!normalizedQuery.isBlank() && title.contains(normalizedQuery)) {
            boost += 0.45d;
        }
        if (!normalizedQuery.isBlank() && text.contains(normalizedQuery)) {
            boost += 0.15d;
        }

        for (String token : queryTokens) {
            if (token.isBlank()) {
                continue;
            }
            if (title.contains(token)) {
                boost += 0.12d;
            }
            if (source.contains(token) || version.contains(token)) {
                boost += 0.06d;
            }
            if (text.contains(token)) {
                boost += 0.02d;
            }
        }
        return boost;
    }

    private List<String> tokenize(String value) {
        return List.of(normalize(value).split("\\s+"));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣()\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private record ScoredDocument(RagRetrievedDocument document, double score) {
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
